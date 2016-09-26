package org.genivi.sota.device_registry

import io.circe.{Json, JsonObject}

import scala.collection.mutable

object JsonComparison {

  def compareJsonObjects(json1: JsonObject, json2: JsonObject): Json = {
    val commonJson = json1.fields.foldLeft(List.empty[(String, Json)]) { (commonJson, key) =>
      json2(key)
        .fold(commonJson) { element =>
          getCommonJson(json1(key).get, element) match {
            case Json.Null => commonJson
            case json      => (key, json) :: commonJson
          }
        }
    }

    commonJson.isEmpty match {
      case true  => Json.Null
      case false => Json.fromFields(commonJson)
    }
  }

  def compareJsonArrays(json1: List[Json], json2: List[Json]): Json = {
    val commonJson = mutable.MutableList.empty[Json]
    val json2Objs = json2.filter(e => e.isObject)
    val json2Arrs = json2.filter(e => e.isArray)
    json1.foreach { json:Json =>
      if(json.isObject) {
        json2Objs.map { e =>
          compareJsonObjects(json.asObject.get, e.asObject.get) match {
            case Json.Null => //ignore null values
            case res       => commonJson += res
          }
        }
      } else if(json.isArray) {
          json2Arrs.map { e =>
            compareJsonArrays(json.asArray.get, e.asArray.get) match {
              case Json.Null => //ignore null values
              case res       => commonJson += res
            }
          }
      }
    }

    commonJson.isEmpty match {
      case true  => Json.Null
      case false => Json.fromValues(commonJson)
    }
  }

  def getCommonJson(json1: Json, json2: Json): Json = {
    if(json1.equals(json2)) {
      json1
    }
    else if(json1.isArray && json2.isArray) {
      compareJsonArrays(json1.asArray.get, json2.asArray.get)
    }
    else if(json1.isObject && json2.isObject) {
      compareJsonObjects(json1.asObject.get, json2.asObject.get)
    }
    else {
      Json.Null
    }
  }

}
