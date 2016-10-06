package org.genivi.sota.device_registry

import akka.http.scaladsl.model.StatusCodes._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import org.genivi.sota.data._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.scalacheck.Arbitrary._
import org.scalatest.FunSuite
import scala.concurrent.ExecutionContext


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

  test("GET all groups lists all groups") {
    //TODO: PRO-1182 turn this back into a property when we can delete groups
    val groups = genGroupInfoList.sample.get
    groups.foreach { case GroupInfo(id, groupName, namespace, groupInfoJson) =>
      createGroupInfoOk(id, groupName, groupInfoJson)
    }

    listGroups() ~> route ~> check {
      //need to sort lists so shouldEqual works
      val resp = responseAs[Seq[GroupInfo]].sortBy(_.groupName.toString)
      val sortedGroups = groups.sortBy(_.groupName.toString)
      resp shouldEqual sortedGroups
      status shouldBe OK
    }
  }

  test("creating groups is possible") {

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
      listDevicesInGroup(groupId) ~> route ~> check {
        status shouldBe OK
        responseAs[Set[Uuid]] should contain allOf (device1, device2)
      }
      listGroupsForDevice(device1) ~> route ~> check {
        status shouldBe OK
        responseAs[Set[Uuid]] should contain (groupId)
      }
      listGroupsForDevice(device2) ~> route ~> check {
        status shouldBe OK
        responseAs[Set[Uuid]] should contain (groupId)
      }
      countDevicesInGroup(groupId) ~> route ~> check {
        status shouldBe OK
        responseAs[Int] shouldEqual 2
      }
    }

  }

  test("GET /group_info request fails on non-existent device") {
    val id = genUuid.sample.get
    fetchGroupInfo(id) ~> route ~> check {
      status shouldBe NotFound
    }
  }

  test("GET group_info after POST should return what was posted.") {
    val group = genGroupInfo.sample.get
    createGroupInfoOk(group.id, group.groupName, group.groupInfo)

    fetchGroupInfo(group.id) ~> route ~> check {
      status shouldBe OK
      val json2: Json = responseAs[Json]
      group.groupInfo shouldEqual json2
    }
  }

  test("GET group_info after PUT should return what was updated.") {
    val id = genUuid.sample.get
    val groupName = genGroupName.sample.get
    val json1 = simpleJsonGen.sample.get
    val json2 = simpleJsonGen.sample.get

    createGroupInfoOk(id, groupName, json1)

    updateGroupInfo(id, groupName, json2) ~> route ~> check {
      status shouldBe OK
    }

    fetchGroupInfo(id) ~> route ~> check {
      status shouldBe OK
      val json3: Json = responseAs[Json]
      json2 shouldBe json3
    }
  }

  test("Renaming groups") {
    val group = genGroupInfo.sample.get
    val newGroupName = genGroupName.sample.get
    createGroupInfoOk(group.id, group.groupName, group.groupInfo)

    renameGroup(group.id, newGroupName) ~> route ~> check {
      status shouldBe OK
    }

    listGroups() ~> route ~> check {
      status shouldBe OK
      val groups = responseAs[Seq[GroupInfo]]
      groups.count(e => e.id.equals(group.id) && e.groupName.equals(newGroupName)) shouldBe 1
    }
  }

  test("counting devices should fail for non-existent groups") {
    countDevicesInGroup(genUuid.sample.get) ~> route ~> check {
      status shouldBe NotFound
    }
  }

  test("match devices to an existing group") {

    val groupInfo = parse("""{"cat":"dog","fish":{"cow":1}}""").getOrElse(Json.Null)
    val systemInfos = Seq(
      parse("""{"cat":"dog","fish":{"cow":1,"sheep":[42,"penguin",23]}}""")
        .getOrElse(Json.Null),
      parse("""{"cat":"dog","fish":{"cow":1,"sloth":true},"antilope":"bison"}""")
        .getOrElse(Json.Null),
      parse("""{"cat":"dog","owl":{"emu":false},"fish":{"cow":1,"pig":"chicken"}}""")
        .getOrElse(Json.Null),
      parse("""{"cat":"liger","fish":{"cow":1}}""")
        .getOrElse(Json.Null)
    )

    val devices@Seq(d1, d2, d3, d4) = genConflictFreeDeviceTs(4).sample.get.map(createDeviceOk(_))
    devices.zip(systemInfos).foreach { case (d, si) => createSystemInfoOk(d, si) }

    createGroupFromDevices(d1, d2, arbitrary[GroupInfo.Name].sample.get) ~> route ~> check {
      status shouldBe OK
      val groupId = responseAs[Uuid]
      fetchGroupInfo(groupId) ~> route ~> check {
        status shouldBe OK
        responseAs[Json] shouldBe groupInfo
      }
      listDevicesInGroup(groupId) ~> route ~> check {
        status shouldBe OK
        val r = responseAs[Seq[Uuid]]
        r should contain (d3)
        r should not contain (d4)
      }
    }
  }
}
