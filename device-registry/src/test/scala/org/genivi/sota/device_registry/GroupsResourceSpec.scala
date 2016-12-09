package org.genivi.sota.device_registry

import akka.http.scaladsl.model.StatusCodes._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import org.genivi.sota.data.{GroupInfo, Uuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalatest.FunSuite
import org.scalatest.concurrent.Eventually.eventually


class GroupsResourceSpec extends FunSuite with ResourceSpec {

  private def createSystemInfoOk(uuid: Uuid, systemInfo: Json) = {
    createSystemInfo(uuid, systemInfo) ~> route ~> check {
      status shouldBe Created
    }
  }

  val complexJsonArray =
    Json.arr(Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromString("fish")))))
  val complexNumericJsonArray =
    Json.arr(Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromInt(5)))))

  val groupInfo = parse("""{"cat":"dog","fish":{"cow":1}}""").toOption.get
  val systemInfos = Seq(
    parse("""{"cat":"dog","fish":{"cow":1,"sheep":[42,"penguin",23]}}"""), // device #1
    parse("""{"cat":"dog","fish":{"cow":1,"sloth":true},"antilope":"bison"}"""), // device #2
    parse("""{"fish":{"cow":1},"cat":"dog"}"""), // matches without discarding
    parse("""{"fish":{"cow":1,"sloth":false},"antilope":"emu","cat":"dog"}"""), // matches with discarding
    parse("""{"cat":"liger","fish":{"cow":1}}"""), // doesn't match without discarding
    parse("""{"cat":"dog","fish":{"cow":1},"bison":17}"""), // doesn't match without discarding
    parse("""{"cat":"dog","fish":{"cow":2,"sheep":false},"antilope":"emu"}""") // doesn't match with discarding
    ).map(_.toOption.get)

  test("GET all groups lists all groups") {
    //TODO: PRO-1182 turn this back into a property when we can delete groups
    val groups = Gen.listOfN(10, arbitrary[GroupInfo]).sample.get
    groups.foreach { group =>
      createGroupInfoOk(group.groupName, group.groupInfo)
    }

    listGroups() ~> route ~> check {
      status shouldBe OK
      val responseGroups = responseAs[Set[GroupInfo]]
      responseGroups.size shouldBe groups.size
      responseGroups.foreach { group =>
        groups.count(p => p.groupName == group.groupName && p.groupInfo == group.groupInfo) shouldBe 1
      }
    }
  }

  test("creating groups manually is possible") {
    val groupName = arbitrary[GroupInfo.Name].sample.get
    createGroup(groupName) ~> route ~> check {
      status shouldBe Created
      val groupId = responseAs[Uuid]
      fetchGroupInfo(groupId) ~> route ~> check {
        status shouldBe OK
        responseAs[Json] shouldEqual Json.Null
      }
    }

  }

  test("creating groups from attributes is possible") {

    val complexJsonObj = Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromString("fish"))))
    val complexNumericJsonObj = Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromInt(5))))

    val groupName = arbitrary[GroupInfo.Name].sample.get

    val device1 = createDeviceOk(genDeviceT.sample.get)
    val device2 = createDeviceOk(genDeviceT.sample.get)

    createSystemInfoOk(device1, complexJsonObj)
    createSystemInfoOk(device2, complexNumericJsonObj)

    createGroupFromDevices(device1, device2, groupName) ~> route ~> check {
      status shouldBe OK
      val groupId = responseAs[Uuid]
      eventually {
        listDevicesInGroup(groupId) ~> route ~> check {
          status shouldBe OK
          responseAs[Set[Uuid]] should contain allOf(device1, device2)
        }
        listGroupsForDevice(device1) ~> route ~> check {
          status shouldBe OK
          responseAs[Set[Uuid]] should contain(groupId)
        }
        listGroupsForDevice(device2) ~> route ~> check {
          status shouldBe OK
          responseAs[Set[Uuid]] should contain(groupId)
        }
        countDevicesInGroup(groupId) ~> route ~> check {
          status shouldBe OK
          responseAs[Int] shouldEqual 2
        }
      }
    }

    deleteDeviceOk(device1)
    deleteDeviceOk(device2)
  }

  test("GET /group_info request fails on non-existent device") {
    val id = genUuid.sample.get
    fetchGroupInfo(id) ~> route ~> check {
      status shouldBe NotFound
    }
  }

  test("GET group_info after POST should return what was posted.") {
    val group = genGroupInfo.sample.get
    val groupId = createGroupInfoOk(group.groupName, group.groupInfo)

    fetchGroupInfo(groupId) ~> route ~> check {
      status shouldBe OK
      val json2: Json = responseAs[Json]
      group.groupInfo shouldEqual json2
    }
  }

  test("GET group_info after PUT should return what was updated.") {
    val groupName = genGroupName.sample.get
    val group = genGroupInfo.sample.get
    val json1 = simpleJsonGen.sample.get
    val json2 = simpleJsonGen.sample.get

    val groupId = createGroupInfoOk(group.groupName, group.groupInfo)

    updateGroupInfo(groupId, groupName, json2) ~> route ~> check {
      status shouldBe OK
    }

    fetchGroupInfo(groupId) ~> route ~> check {
      status shouldBe OK
      val json3: Json = responseAs[Json]
      json2 shouldBe json3
    }
  }

  test("Renaming groups") {
    val group = genGroupInfo.sample.get
    val newGroupName = genGroupName.sample.get
    val groupId = createGroupInfoOk(group.groupName, group.groupInfo)

    renameGroup(groupId, newGroupName) ~> route ~> check {
      status shouldBe OK
    }

    listGroups() ~> route ~> check {
      status shouldBe OK
      val groups = responseAs[Seq[GroupInfo]]
      groups.count(e => e.id.equals(groupId) && e.groupName.equals(newGroupName)) shouldBe 1
    }
  }

  test("counting devices should fail for non-existent groups") {
    countDevicesInGroup(genUuid.sample.get) ~> route ~> check {
      status shouldBe NotFound
    }
  }

  test("match devices to an existing group") {
    val devices@Seq(d1, d2, d3, d4, d5, d6, d7) =
      genConflictFreeDeviceTs(7).sample.get.map(createDeviceOk(_))
    devices.zip(systemInfos).foreach { case (d, si) => createSystemInfoOk(d, si) }

    createGroupFromDevices(d1, d2, arbitrary[GroupInfo.Name].sample.get) ~> route ~> check {
      status shouldBe OK
      val groupId = responseAs[Uuid]
      fetchGroupInfo(groupId) ~> route ~> check {
        status shouldBe OK
        responseAs[Json] shouldBe groupInfo
      }
      eventually {
        listDevicesInGroup(groupId) ~> route ~> check {
          status shouldBe OK
          val r = responseAs[Seq[Uuid]]
          r should contain (d3)
          r should contain (d4)
          r should not contain (d5)
          r should not contain (d6)
          r should not contain (d7)
        }
      }
    }

    devices.foreach(deleteDeviceOk(_))
  }

  test("adding devices to groups") {
    val group = genGroupInfo.sample.get
    val deviceId = createDeviceOk(genDeviceT.sample.get)
    val groupId = createGroupInfoOk(group.groupName, group.groupInfo)

    addDeviceToGroup(groupId, deviceId) ~> route ~> check {
      status shouldBe OK
    }

    listDevicesInGroup(groupId) ~> route ~> check {
      status shouldBe OK
      val devices = responseAs[Seq[Uuid]]
      devices.contains(deviceId) shouldBe true
    }

    deleteDeviceOk(deviceId)
  }

  test("removing devices from groups") {
    val group = genGroupInfo.sample.get
    val deviceId = createDeviceOk(genDeviceT.sample.get)
    val groupId = createGroupInfoOk(group.groupName, group.groupInfo)

    addDeviceToGroup(groupId, deviceId) ~> route ~> check {
      status shouldBe OK
    }

    listDevicesInGroup(groupId) ~> route ~> check {
      status shouldBe OK
      val devices = responseAs[Seq[Uuid]]
      devices.contains(deviceId) shouldBe true
    }

    removeDeviceFromGroup(groupId, deviceId) ~> route ~> check {
      status shouldBe OK
    }

    listDevicesInGroup(groupId) ~> route ~> check {
      status shouldBe OK
      val devices = responseAs[Seq[Uuid]]
      devices.contains(deviceId) shouldBe false
    }

    deleteDeviceOk(deviceId)
  }
}
