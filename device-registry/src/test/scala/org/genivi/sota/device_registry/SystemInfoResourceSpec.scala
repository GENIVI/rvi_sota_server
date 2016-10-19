/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry

import io.circe.Json
import org.genivi.sota.data.DeviceGenerators._
import org.genivi.sota.data.SimpleJsonGenerator._
import org.genivi.sota.data._
import org.genivi.sota.marshalling.CirceMarshallingSupport._

class SystemInfoResourceSpec extends ResourcePropSpec {

  import UuidGenerator._
  import akka.http.scaladsl.model.StatusCodes._

  def removeIdNr(json: Json): Json = json.arrayOrObject(
    json,
    x => Json.fromValues(x.map(removeIdNr)),
    x => Json.fromFields(x.toList.map{case (i,v) => (i, removeIdNr(v))}.filter{case (i,_) => i != "id-nr"})
  )


  property("GET /system_info request fails on non-existent device") {
    forAll { (uuid: Uuid, json: Json) =>
      fetchSystemInfo(uuid)        ~> route ~> check { status shouldBe NotFound }
      createSystemInfo(uuid, json) ~> route ~> check { status shouldBe NotFound}
      updateSystemInfo(uuid, json) ~> route ~> check { status shouldBe NotFound}
    }
  }

  property("GET /system_info return empty if device have not set system_info") {
    forAll { device: DeviceT =>
      val uuid: Uuid = createDeviceOk(device)

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val json = responseAs[Json]

        json shouldBe Json.obj()
      }

      deleteDeviceOk(uuid)
    }
  }

  property("GET system_info after POST should return what was posted.") {
    forAll { (device: DeviceT, json1: Json) =>
      val uuid: Uuid = createDeviceOk(device)

      createSystemInfo(uuid, json1) ~> route ~> check {
        status shouldBe Created
      }

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val json2: Json = responseAs[Json]
        json1 shouldBe removeIdNr(json2)
      }

      deleteDeviceOk(uuid)
    }
  }

  property("GET system_info after PUT should return what was updated.") {
    forAll { (device: DeviceT, json1: Json, json2: Json) =>
      val uuid: Uuid = createDeviceOk(device)

      createSystemInfo(uuid, json1) ~> route ~> check {
        status shouldBe Created
      }

      updateSystemInfo(uuid, json2) ~> route ~> check {
        status shouldBe OK
      }

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val json3: Json = responseAs[Json]
        json2 shouldBe removeIdNr(json3)
      }

      deleteDeviceOk(uuid)
    }
  }

  property("PUT system_info if not previously created should create it.") {
    forAll { (device: DeviceT, json: Json) =>
      val uuid: Uuid = createDeviceOk(device)

      updateSystemInfo(uuid, json) ~> route ~> check {
        status shouldBe OK
      }

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val json2: Json = responseAs[Json]
        json shouldBe removeIdNr(json2)
      }

      deleteDeviceOk(uuid)
    }
  }

  property("system_info adds unique numbers for each id-field") {
    def addId(json: Json): Json = json.arrayOrObject(
      json,
      x => Json.fromValues(x.map(addId)),
      x => Json.fromFields(("uuid" -> Json.fromString("uuid")) :: x.toList.map{case (i,v) => (i, addId(v))})
    )

    def getField(field: String)(json: Json): Seq[Json] = json.arrayOrObject(
      List(),
      _.flatMap(getField(field)),
      x => x.toList.flatMap {
        case (i, v) if i == field => List(v)
        case (_, v) => getField(field)(v)
      })

    forAll{ (device: DeviceT, json: Json) =>
      val uuid: Uuid = createDeviceOk(device)

      val jsonWithId: Json = addId(json)

      updateSystemInfo(uuid, jsonWithId) ~> route ~> check {
        status shouldBe OK
      }

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val retJson = responseAs[Json]
        jsonWithId shouldBe removeIdNr(retJson)

        val ids = getField("id")(retJson)
        val idNrs = getField("id-nr")(retJson)
        //unique
        idNrs.size shouldBe idNrs.toSet.size

        //same count
        ids.size shouldBe idNrs.size
      }

      deleteDeviceOk(uuid)
    }
  }
}
