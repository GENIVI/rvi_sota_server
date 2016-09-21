package org.genivi.sota.device_registry

import io.circe.Json
import org.scalatest.{FunSuite, Matchers}

class JsonComparisonSpec extends FunSuite with Matchers {

  val simpleJsonObj = Json.obj(("key", Json.fromString("value")))
  val simpleJsonArray = Json.obj(("key", Json.fromString("value")))
  val keyOnlyObj = Json.obj(("key", Json.Null))
  val complexJsonObj = Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromString("fish"))))
  val complexNumericJsonObj = Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromInt(5))))
  val complexJsonArray =
    Json.arr(Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromString("fish")))))
  val complexNumericJsonArray =
    Json.arr(Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromInt(5)))))

  test("json comparison of two empty sets returns empty json object") {
    JsonComparison.getCommonJson(Json.Null, Json.Null) shouldEqual  Json.Null
  }

  test("json comparison of two identical json objects returns same object") {
    JsonComparison.getCommonJson(simpleJsonObj, simpleJsonObj) shouldEqual  simpleJsonObj
  }

  test("json comparison of two different json objects returns null") {
    JsonComparison.getCommonJson(simpleJsonObj, keyOnlyObj) shouldEqual  Json.Null
  }

  test("json comparison of two similar json objects returns common json") {
    JsonComparison.getCommonJson(simpleJsonObj, complexJsonObj) shouldEqual  simpleJsonObj
  }

  test("json comparison of two json objects with different value types returns common json") {
    JsonComparison.getCommonJson(complexJsonObj, complexNumericJsonObj) shouldEqual simpleJsonObj
  }

  test("json comparison of two empty json arrays returns null") {
    JsonComparison.getCommonJson(Json.arr(Json.Null), Json.arr(Json.Null)) shouldEqual Json.arr(Json.Null)
  }

  test("json comparison of two simple json arrays returns common json") {
    JsonComparison.getCommonJson(Json.arr(simpleJsonObj), Json.arr(simpleJsonObj)) shouldEqual Json.arr(simpleJsonObj)
  }

  test("json comparison of two json arrays with different value types returns common json") {
    JsonComparison.getCommonJson(complexJsonArray, complexNumericJsonArray) shouldEqual Json.arr(simpleJsonObj)
  }

}
