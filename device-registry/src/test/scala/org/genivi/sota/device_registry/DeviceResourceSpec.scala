/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import akka.http.scaladsl.model.StatusCodes
import io.circe.generic.auto._
import io.circe.Json
import org.genivi.sota.data._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import java.time.Instant
import java.time.temporal.ChronoUnit

import org.scalacheck._

/**
 * Spec for DeviceRepository REST actions
 */
class DeviceResourceSpec extends ResourcePropSpec {

  import Arbitrary._
  import Device._
  import DeviceGenerators._
  import GroupInfoGenerators._
  import SimpleJsonGenerator._
  import StatusCodes._
  import UuidGenerator._

  def removeIdNr(json: Json): Json = json.arrayOrObject(
    json,
    x => Json.fromValues(x.map(removeIdNr)),
    x => Json.fromFields(x.toList.map{case (i,v) => (i, removeIdNr(v))}.filter{case (i,_) => i != "id-nr"})
  )

  def createDeviceOk(device: DeviceT): Uuid = {
    createDevice(device) ~> route ~> check {
      status shouldBe Created
      responseAs[Uuid]
    }
  }

  def deleteDeviceOk(uuid: Uuid): Unit = {
    deleteDevice(uuid) ~> route ~> check {
      status shouldBe OK
    }
  }

  def isRecent(time: Option[Instant]): Boolean = time match {
    case Some(time) => time.isAfter(Instant.now.minus(3, ChronoUnit.MINUTES))
    case None => false
  }

  property("GET, PUT, DELETE, and POST '/ping' request fails on non-existent device") {
    forAll { (uuid: Uuid, device: DeviceT, json: Json) =>
      fetchDevice(uuid)          ~> route ~> check { status shouldBe NotFound }
      updateDevice(uuid, device) ~> route ~> check { status shouldBe NotFound }
      deleteDevice(uuid)         ~> route ~> check { status shouldBe NotFound }
      updateLastSeen(uuid)       ~> route ~> check { status shouldBe NotFound }
    }
  }

  property("GET /system_info request fails on non-existent device") {
    forAll { (uuid: Uuid, json: Json) =>
      fetchSystemInfo(uuid)      ~> route ~> check { status shouldBe NotFound }
      createSystemInfo(uuid, json) ~> route ~> check { status shouldBe NotFound}
      updateSystemInfo(uuid, json) ~> route ~> check { status shouldBe NotFound}
    }
  }

  property("GET /group_info request fails on non-existent device") {
    forAll(genUuid) { id =>
      fetchGroupInfo (id) ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  property("GET request (for Id) after POST yields same device") {
    forAll { devicePre: DeviceT =>
      val uuid: Uuid = createDeviceOk(devicePre)

      fetchDevice(uuid) ~> route ~> check {
        status shouldBe OK
        val devicePost: Device = responseAs[Device]
        devicePost.deviceId shouldBe devicePre.deviceId
        devicePost.deviceType shouldBe devicePre.deviceType
        devicePost.lastSeen shouldBe None
      }

      deleteDeviceOk(uuid)
    }
  }

  property("GET request with ?deviceId after creating yields same device.") {
    forAll { (deviceId: DeviceId, devicePre: DeviceT) =>

      val uuid: Uuid = createDeviceOk(devicePre.copy(deviceId = Some(deviceId)))
      fetchByDeviceId(defaultNs, deviceId) ~> route ~> check {
        status shouldBe OK
        val devicePost1: Device = responseAs[Seq[Device]].head
        fetchDevice(uuid) ~> route ~> check {
          status shouldBe OK
          val devicePost2: Device = responseAs[Device]

          devicePost1 shouldBe devicePost2
        }
      }
      deleteDeviceOk(uuid)
    }
  }

  property("PUT request after POST succeeds with updated device.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1, d2) =>

      val uuid: Uuid = createDeviceOk(d1)

      updateDevice(uuid, d2) ~> route ~> check {
        val updateStatus = status

        d1.deviceId match {
          case Some(deviceId) =>
            fetchByDeviceId(defaultNs, deviceId) ~> route ~> check {
              status match {
                case OK => responseAs[Seq[Device]].headOption match {
                  case Some(_) => updateStatus shouldBe Conflict
                  case None =>
                    updateStatus shouldBe OK

                    fetchDevice(uuid) ~> route ~> check {
                      status shouldBe OK
                      val devicePost: Device = responseAs[Device]
                      devicePost.uuid shouldBe uuid
                      devicePost.deviceId shouldBe d2.deviceId
                      devicePost.deviceType shouldBe d2.deviceType
                      devicePost.lastSeen shouldBe None
                    }
                }
                case _ => assert(false, "unexpected status code: " + status)
              }
            }
          case None => updateStatus shouldBe OK
        }
      }

      deleteDeviceOk(uuid)
    }
  }

  property("POST request creates a new device.") {
    forAll { devicePre: DeviceT =>

      val uuid: Uuid = createDeviceOk(devicePre)

      fetchDevice(uuid) ~> route ~> check {
        status shouldBe OK
        val devicePost: Device = responseAs[Device]
        devicePost.uuid shouldBe uuid
        devicePost.deviceId shouldBe devicePre.deviceId
        devicePost.deviceType shouldBe devicePre.deviceType
      }

      deleteDeviceOk(uuid)
    }
  }

  property("DELETE request after POST succeds and deletes device.") {
    forAll { (device: DeviceT) =>

      val uuid: Uuid = createDeviceOk(device)

      deleteDevice(uuid) ~> route ~> check {
        status shouldBe OK
      }

      fetchDevice(uuid) ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  property("POST request on 'ping' should update 'lastSeen' field for device.") {
    forAll { (uuid: Uuid, devicePre: DeviceT) =>

      val uuid: Uuid = createDeviceOk(devicePre)

      updateLastSeen(uuid) ~> route ~> check {
        status shouldBe OK
      }

      fetchDevice(uuid) ~> route ~> check {
        val devicePost: Device = responseAs[Device]
        val after: Instant = Instant.now()

        devicePost.lastSeen should not be (None)
        isRecent(devicePost.lastSeen) shouldBe true
      }

      deleteDeviceOk(uuid)
    }
  }

  property("POST request with same deviceName fails with conflict.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1, d2) =>

      val name = arbitrary[DeviceName].sample.get
      val uuid: Uuid = createDeviceOk(d1.copy(deviceName = name))

      createDevice(d2.copy(deviceName = name)) ~> route ~> check {
        status shouldBe Conflict
      }

      deleteDeviceOk(uuid)
    }
  }

  property("POST request with same deviceId fails with conflict.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1, d2) =>

      val uuid: Uuid = createDeviceOk(d1)

      createDevice(d2.copy(deviceId = d1.deviceId)) ~> route ~> check {
        d1.deviceId match {
          case Some(deviceId) => status shouldBe Conflict
          case None => deleteDeviceOk(responseAs[Uuid])
        }
      }

      deleteDeviceOk(uuid)
    }
  }

  property("PUT request updates device.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1: DeviceT, d2: DeviceT) =>

      val uuid: Uuid = createDeviceOk(d1)

      updateDevice(uuid, d2) ~> route ~> check {
        status shouldBe OK
        fetchDevice(uuid) ~> route ~> check {
          status shouldBe OK
          val updatedDevice: Device = responseAs[Device]
          updatedDevice.deviceId shouldBe d2.deviceId
          updatedDevice.deviceType shouldBe d2.deviceType
          updatedDevice.lastSeen shouldBe None
        }
      }

      deleteDeviceOk(uuid)
    }
  }

  property("PUT request with same deviceName fails with conflict.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1, d2) =>

      val uuid1: Uuid = createDeviceOk(d1)
      val uuid2: Uuid = createDeviceOk(d2)

      updateDevice(uuid1, d1.copy(deviceName = d2.deviceName)) ~> route ~> check {
        status shouldBe Conflict
      }

      deleteDeviceOk(uuid1)
      deleteDeviceOk(uuid2)
    }
  }

  property("PUT request with same deviceId fails with conflict.") {
    forAll(genConflictFreeDeviceTs(2)) { case Seq(d1, d2) =>

      val deviceId = arbitrary[DeviceId].suchThat(_ != d1.deviceId).sample.get
      val uuid1: Uuid = createDeviceOk(d1)
      val uuid2: Uuid = createDeviceOk(d2.copy(deviceId = Some(deviceId)))

      updateDevice(uuid1, d1.copy(deviceId = Some(deviceId))) ~> route ~> check {
        d2.deviceId match {
          case Some(deviceId) => status shouldBe Conflict
          case None => ()
        }
      }

      deleteDeviceOk(uuid1)
      deleteDeviceOk(uuid2)
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

  property("GET all groups lists all groups") {
    //TODO: PRO-1182 turn this back into a property when we can delete groups
    val groups = genGroupInfoList.sample.get
    groups.foreach { case GroupInfo(id, groupName, namespace, groupInfoJson) =>
      createGroupInfo(id, groupName, groupInfoJson) ~> route ~> check {
        status shouldBe Created
      }
    }

    listGroups() ~> route ~> check {
      //need to sort lists so shouldEqual works
      val resp = responseAs[Seq[GroupInfo]].sortBy(_.groupName.toString)
      val sortedGroups = groups.sortBy(_.groupName.toString)
      resp shouldEqual sortedGroups
      status shouldBe OK
    }
  }

  property("GET group_info after POST should return what was posted.") {
    forAll(genGroupInfo) { group =>
      createGroupInfo(group.id, group.groupName, group.groupInfo) ~> route ~> check {
        status shouldBe Created
      }

      fetchGroupInfo(group.id) ~> route ~> check {
        status shouldBe OK
        val json2: Json = responseAs[Json]
        group.groupInfo shouldEqual json2
      }
    }
  }

  property("GET group_info after PUT should return what was updated.") {
    forAll(genUuid, genGroupName, simpleJsonGen, simpleJsonGen) { (id, groupName, json1, json2) =>
      whenever(!json1.isNull && !json2.isNull) {
        createGroupInfo(id, groupName, json1) ~> route ~> check {
          status shouldBe Created
        }

        updateGroupInfo(id, groupName, json2) ~> route ~> check {
          status shouldBe OK
        }

        fetchGroupInfo(id) ~> route ~> check {
          status shouldBe OK
          val json3: Json = responseAs[Json]
          json2 shouldBe json3
        }
      }
    }
  }

  property("Renaming groups") {
    forAll(genGroupInfo, genGroupName) { (group, newGroupName) =>
      createGroupInfo(group.id, group.groupName, group.groupInfo) ~> route ~> check {
        status shouldBe Created
      }

      renameGroup(group.id, newGroupName) ~> route ~> check {
        status shouldBe OK
      }

      listGroups() ~> route ~> check {
        status shouldBe OK
        val groups = responseAs[Seq[GroupInfo]]
        groups.count(e => e.id.equals(group.id) && e.groupName.equals(newGroupName)) shouldBe 1
      }
    }
  }
}
