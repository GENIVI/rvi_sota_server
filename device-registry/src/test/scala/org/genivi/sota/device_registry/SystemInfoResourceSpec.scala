/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.device_registry

import io.circe.Json
import org.genivi.sota.data.DeviceGenerators._
import org.genivi.sota.data.SimpleJsonGenerator._
import org.genivi.sota.data._
import org.genivi.sota.device_registry.db.SystemInfoRepository.removeIdNrs
import org.genivi.sota.marshalling.CirceMarshallingSupport._

class SystemInfoResourceSpec extends ResourcePropSpec {

  import UuidGenerator._
  import akka.http.scaladsl.model.StatusCodes._

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
    forAll { (device: DeviceT, json0: Json) =>
      val uuid: Uuid = createDeviceOk(device)
      val json1: Json = removeIdNrs(json0)

      createSystemInfo(uuid, json1) ~> route ~> check {
        status shouldBe Created
      }

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val json2: Json = responseAs[Json]
        json1 shouldBe removeIdNrs(json2)
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
        json2 shouldBe removeIdNrs(json3)
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
        json shouldBe removeIdNrs(json2)
      }

      deleteDeviceOk(uuid)
    }
  }

  property("system_info adds unique numbers for each json-object") {
    def countObjects(json: Json): Int = json.arrayOrObject(
      0,
      x => x.map(countObjects).sum,
      x => x.toList.map{case (_,v) => countObjects(v)}.sum + 1
    )

    def getField(field: String)(json: Json): Seq[Json] = json.arrayOrObject(
      List(),
      _.flatMap(getField(field)),
      x => x.toList.flatMap {
        case (i, v) if i == field => List(v)
        case (_, v) => getField(field)(v)
      })

    forAll{ (device: DeviceT, json0: Json) =>
      val uuid: Uuid = createDeviceOk(device)
      val json = removeIdNrs(json0)

      updateSystemInfo(uuid, json) ~> route ~> check {
        status shouldBe OK
      }

      fetchSystemInfo(uuid) ~> route ~> check {
        status shouldBe OK
        val retJson = responseAs[Json]
        json shouldBe removeIdNrs(retJson)

        val idNrs = getField("id-nr")(retJson)
        //unique
        idNrs.size shouldBe idNrs.toSet.size

        //same count
        countObjects(json) shouldBe idNrs.size
      }

      deleteDeviceOk(uuid)
    }
  }
}
