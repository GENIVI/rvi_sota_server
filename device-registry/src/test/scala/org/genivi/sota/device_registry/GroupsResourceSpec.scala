package org.genivi.sota.device_registry

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.testkit.ScalatestRouteTest
import eu.timepit.refined.api.Refined
import io.circe.Json
import org.genivi.sota.core.DatabaseSpec
import org.genivi.sota.data.GroupInfo.Name
import org.genivi.sota.data.{DeviceT, GroupInfo, Uuid}
import org.genivi.sota.device_registry.common.CreateGroupRequest
import org.scalatest.{FunSuite, Matchers}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import io.circe.generic.auto._
import io.circe.syntax._
import org.genivi.sota.data.UuidGenerator._
import org.genivi.sota.data.GroupInfoGenerators._
import org.genivi.sota.data.SimpleJsonGenerator._

import scala.concurrent.ExecutionContext

class GroupsResourceSpec extends FunSuite
    with ScalatestRouteTest
    with Matchers
    with GroupRequests
    with ResourceSpec
    with DatabaseSpec {

  import akka.http.scaladsl.model.StatusCodes._

  def createDeviceOk(device: DeviceT): Uuid = {
    createDevice(device) ~> route ~> check {
      status shouldBe Created
      responseAs[Uuid]
    }
  }

  private def createSystemInfoOk(uuid: Uuid, systemInfo: Json) = {
    createSystemInfo(uuid, systemInfo) ~> route ~> check {
      status shouldBe Created
    }
  }

  def createGroupFromDevices(device1: Uuid, device2: Uuid, groupName: Name)
                            (implicit ec: ExecutionContext): HttpRequest = {
    Post(Resource.uri("device_groups", "from_attributes"), CreateGroupRequest(device1, device2, groupName).asJson)
  }

  def listDevicesInGroup(groupId: Uuid)(implicit ec: ExecutionContext): HttpRequest =
    Get(Resource.uri("device_groups", groupId.underlying.get, "devices"))

  def countDevicesInGroup(groupId: Uuid)(implicit ec: ExecutionContext): HttpRequest =
    Get(Resource.uri("device_groups", groupId.underlying.get, "count"))

  val complexJsonArray =
    Json.arr(Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromString("fish")))))
  val complexNumericJsonArray =
    Json.arr(Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromInt(5)))))

  test("GET all groups lists all groups") {
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

  test("creating groups is possible") {

    import org.genivi.sota.data.DeviceGenerators.genDeviceT

    val complexJsonObj = Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromString("fish"))))
    val complexNumericJsonObj = Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromInt(5))))

    val groupName = genGroupName.sample.get

    val device1 = createDeviceOk(genDeviceT.sample.get)
    val device2 = createDeviceOk(genDeviceT.sample.get)

    createSystemInfoOk(device1, complexJsonObj)
    createSystemInfoOk(device2, complexNumericJsonObj)

    createGroupFromDevices(device1, device2, groupName) ~> route ~> check {
      status shouldBe OK
      val groupId = responseAs[Uuid]
      listDevicesInGroup(groupId) ~> route ~> check {
        status shouldBe OK
        val expectedResult: Set[Uuid] = Set(device1, device2)
        responseAs[Set[Uuid]] shouldEqual expectedResult
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
    createGroupInfo(group.id, group.groupName, group.groupInfo) ~> route ~> check {
      status shouldBe Created
    }

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

  test("Renaming groups") {
    val group = genGroupInfo.sample.get
    val newGroupName = genGroupName.sample.get
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

  test("counting devices should fail for non-existent groups") {
    countDevicesInGroup(genUuid.sample.get) ~> route ~> check {
      status shouldBe NotFound
    }
  }
}
