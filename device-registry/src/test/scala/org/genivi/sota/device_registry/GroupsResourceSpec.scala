package org.genivi.sota.device_registry

import akka.http.scaladsl.model.StatusCodes._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import org.genivi.sota.data.{Group, Uuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalatest.FunSuite


class GroupsResourceSpec extends FunSuite with ResourceSpec {

  private def createSystemInfoOk(uuid: Uuid, systemInfo: Json) = {
    createSystemInfo(uuid, systemInfo) ~> route ~> check {
      status shouldBe Created
    }
  }

  private val complexJsonArray =
    Json.arr(Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromString("fish")))))
  private val complexNumericJsonArray =
    Json.arr(Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromInt(5)))))

  private val groupInfo = parse("""{"cat":"dog","fish":{"cow":1}}""").toOption.get
  private val systemInfos = Seq(
    parse("""{"cat":"dog","fish":{"cow":1,"sheep":[42,"penguin",23]}}"""), // device #1
    parse("""{"cat":"dog","fish":{"cow":1,"sloth":true},"antilope":"bison"}"""), // device #2
    parse("""{"fish":{"cow":1},"cat":"dog"}"""), // matches without discarding
    parse("""{"fish":{"cow":1,"sloth":false},"antilope":"emu","cat":"dog"}"""), // matches with discarding
    parse("""{"cat":"liger","fish":{"cow":1}}"""), // doesn't match without discarding
    parse("""{"cat":"dog","fish":{"cow":1},"bison":17}"""), // doesn't match without discarding
    parse("""{"cat":"dog","fish":{"cow":2,"sheep":false},"antilope":"emu"}""") // doesn't match with discarding
    ).map(_.toOption.get)
  private val limit = 30

  test("GET all groups lists all groups") {
    //TODO: PRO-1182 turn this back into a property when we can delete groups
    val groupNames = Gen.listOfN(10, arbitrary[Group.Name]).sample.get
    groupNames.foreach { groupName =>
      createGroupOk(groupName)
    }

    listGroups() ~> route ~> check {
      status shouldBe OK
      val responseGroups = responseAs[Set[Group]]
      responseGroups.size shouldBe groupNames.size
      responseGroups.foreach { group =>
        groupNames.count(name => name == group.groupName) shouldBe 1
      }
    }
  }

  test("can list all devices in a group by not providing an offset or limit") {
    val defaultPaginationLimit = 50
    val deviceNumber = defaultPaginationLimit + 10

    val groupName = genGroupName.sample.get
    val groupId = createGroupOk(groupName)

    val deviceTs = genConflictFreeDeviceTs(deviceNumber).sample.get
    val deviceIds: Seq[Uuid] = deviceTs.map(createDeviceOk(_))

    deviceIds.foreach(deviceId => addDeviceToGroupOk(groupId, deviceId))

    listDevicesInGroup(groupId) ~> route ~> check {
      status shouldBe OK
      val result = responseAs[Seq[Uuid]]
      result.length shouldBe deviceNumber
    }
  }

  test("can list devices with custom pagination limit") {
    val deviceNumber = 50
    val group = genGroupInfo.sample.get
    val groupId = createGroupOk(group.groupName)

    val deviceTs = genConflictFreeDeviceTs(deviceNumber).sample.get
    val deviceIds: Seq[Uuid] = deviceTs.map(createDeviceOk(_))

    deviceIds.foreach(deviceId => addDeviceToGroupOk(groupId, deviceId))

    listDevicesInGroup(groupId, limit = Some(limit)) ~> route ~> check {
      status shouldBe OK
      val result = responseAs[Seq[Uuid]]
      result.length shouldBe limit
    }
  }

  test("can list devices with custom pagination limit and offset") {
    val offset = 10
    val deviceNumber = 50
    val group = genGroupInfo.sample.get
    val groupId = createGroupOk(group.groupName)

    val deviceTs = genConflictFreeDeviceTs(deviceNumber).sample.get
    val deviceIds: Seq[Uuid] = deviceTs.map(createDeviceOk(_))

    deviceIds.foreach(deviceId => addDeviceToGroupOk(groupId, deviceId))

    val allDevices = listDevicesInGroup(groupId, limit = Some(deviceNumber)) ~> route ~> check {
      responseAs[Seq[Uuid]]
    }

    listDevicesInGroup(groupId, offset = Some(offset), limit = Some(limit)) ~> route ~> check {
      status shouldBe OK
      val result = responseAs[Seq[Uuid]]
      result.length shouldBe limit
      allDevices.slice(offset, offset + limit) shouldEqual result
    }
  }

  test("Renaming groups") {
    val groupName = genGroupName.sample.get
    val newGroupName = genGroupName.sample.get
    val groupId = createGroupOk(groupName)

    renameGroup(groupId, newGroupName) ~> route ~> check {
      status shouldBe OK
    }

    listGroups() ~> route ~> check {
      status shouldBe OK
      val groups = responseAs[Seq[Group]]
      groups.count(e => e.id.equals(groupId) && e.groupName.equals(newGroupName)) shouldBe 1
    }
  }

  test("counting devices should fail for non-existent groups") {
    countDevicesInGroup(genUuid.sample.get) ~> route ~> check {
      status shouldBe NotFound
    }
  }

  test("adding devices to groups") {
    val groupName = genGroupName.sample.get
    val deviceId = createDeviceOk(genDeviceT.sample.get)
    val groupId = createGroupOk(groupName)

    addDeviceToGroup(groupId, deviceId) ~> route ~> check {
      status shouldBe OK
    }

    listDevicesInGroup(groupId) ~> route ~> check {
      status shouldBe OK
      val devices = responseAs[Seq[Uuid]]
      devices.contains(deviceId) shouldBe true
    }
  }

  test("removing devices from groups") {
    val groupName = genGroupName.sample.get
    val deviceId = createDeviceOk(genDeviceT.sample.get)
    val groupId = createGroupOk(groupName)

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
  }

}
