package org.genivi.sota.device_registry

import io.circe.Json
import io.circe.parser._
import org.scalatest.{FunSuite, Matchers}

class JsonMatcherSpec extends FunSuite with Matchers {

  import JsonMatcher._

  val simpleJsonObj           = parse(""" { "key": "value" }                          """).toOption.get
  val keyOnlyObj              = parse(""" { "key": null }                             """).toOption.get
  val complexJsonObj          = parse(""" { "key": "value", "type": "fish" }          """).toOption.get
  val complexNumericJsonObj   = parse(""" { "key": "value", "type": 5 }               """).toOption.get
  val complexJsonArray        = parse(""" [ { "key": "value", "type": "fish" } ]      """).toOption.get
  val complexNumericJsonArray = parse(""" [ { "key": "value", "type": 5 } ]           """).toOption.get
  val discardedAttrs1         = parse(""" { "type": null}                             """).toOption.get
  val discardedAttrs2         = parse(""" { "key": null, "type": null}                """).toOption.get
  val discardedAttrs3         = parse(""" { "key": null, "type": null, "fish": null } """).toOption.get
  val discardedAttrs4         = parse(""" { "type": null, "fish": null }              """).toOption.get

  test("json comparison of two empty sets returns empty json object") {
    compare(Json.Null, Json.Null)
      .shouldEqual((Json.Null, Json.Null))
  }

  test("json comparison of two identical json objects returns same object") {
    compare(simpleJsonObj, simpleJsonObj)
      .shouldEqual((simpleJsonObj, Json.Null))
  }

  test("json comparison of two different json objects returns null") {
    compare(simpleJsonObj, keyOnlyObj)
      .shouldEqual((Json.Null, keyOnlyObj))
  }

  test("json comparison of two similar json objects returns common json") {
    compare(simpleJsonObj, complexJsonObj)
      .shouldEqual((simpleJsonObj, Json.obj("type" -> Json.Null)))
  }

  test("json comparison of two json objects with different value types returns common json") {
    compare(complexJsonObj, complexNumericJsonObj)
      .shouldEqual((simpleJsonObj, Json.obj("type" -> Json.Null)))
  }

  test("json comparison of two empty json arrays returns an empty array") {
    compare(Json.arr(), Json.arr())
      .shouldEqual((Json.arr(), Json.Null))
  }

  test("json comparison of two simple json arrays returns common json") {
    compare(Json.arr(simpleJsonObj), Json.arr(simpleJsonObj))
      .shouldEqual((Json.arr(simpleJsonObj), Json.Null))
  }

  test("json comparison of two json arrays with different value types returns common json") {
    compare(complexJsonArray, complexNumericJsonArray)
      .shouldEqual((Json.arr(simpleJsonObj), Json.Null))
  }

  test("disregarding a subset of attributes should yield a subset") {
    disregard(complexJsonObj, discardedAttrs1) shouldBe simpleJsonObj
  }

  test("disregarding all attributes should yield an empty set") {
    disregard(complexJsonObj, discardedAttrs2) shouldBe Json.obj()
  }

  test("disregarding a superset of attributes should yield an empty set") {
    disregard(complexJsonObj, discardedAttrs3) shouldBe Json.obj()
  }

  test("disregarding a disjoint set of attributes should yield a subset") {
    disregard(complexJsonObj, discardedAttrs4) shouldBe simpleJsonObj
  }
}
