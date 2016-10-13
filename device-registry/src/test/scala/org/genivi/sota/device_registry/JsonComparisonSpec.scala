package org.genivi.sota.device_registry

import io.circe.Json
import io.circe.parser._
import org.scalatest.{FunSuite, Matchers}

class JsonMatcherSpec extends FunSuite with Matchers {

  val simpleJsonObj           = parse(""" { "key": "value" }                     """).toOption.get
  val keyOnlyObj              = parse(""" { "key": null }                        """).toOption.get
  val complexJsonObj          = parse(""" { "key": "value", "type": "fish" }     """).toOption.get
  val complexNumericJsonObj   = parse(""" { "key": "value", "type": 5 }          """).toOption.get
  val complexJsonArray        = parse(""" [ { "key": "value", "type": "fish" } ] """).toOption.get
  val complexNumericJsonArray = parse(""" [ { "key": "value", "type": 5 } ]      """).toOption.get

  test("json comparison of two empty sets returns empty json object") {
    JsonMatcher.compare(Json.Null, Json.Null)
      .shouldEqual((Json.Null, Json.Null))
  }

  test("json comparison of two identical json objects returns same object") {
    JsonMatcher.compare(simpleJsonObj, simpleJsonObj)
      .shouldEqual((simpleJsonObj, Json.Null))
  }

  test("json comparison of two different json objects returns null") {
    JsonMatcher.compare(simpleJsonObj, keyOnlyObj)
      .shouldEqual((Json.Null, keyOnlyObj))
  }

  test("json comparison of two similar json objects returns common json") {
    JsonMatcher.compare(simpleJsonObj, complexJsonObj)
      .shouldEqual((simpleJsonObj, Json.obj("type" -> Json.Null)))
  }

  test("json comparison of two json objects with different value types returns common json") {
    JsonMatcher.compare(complexJsonObj, complexNumericJsonObj)
      .shouldEqual((simpleJsonObj, Json.obj("type" -> Json.Null)))
  }

  test("json comparison of two empty json arrays returns an empty array") {
    JsonMatcher.compare(Json.arr(), Json.arr())
      .shouldEqual((Json.arr(), Json.Null))
  }

  test("json comparison of two simple json arrays returns common json") {
    JsonMatcher.compare(Json.arr(simpleJsonObj), Json.arr(simpleJsonObj))
      .shouldEqual((Json.arr(simpleJsonObj), Json.Null))
  }

  test("json comparison of two json arrays with different value types returns common json") {
    JsonMatcher.compare(complexJsonArray, complexNumericJsonArray)
      .shouldEqual((Json.arr(simpleJsonObj), Json.Null))
  }

}
