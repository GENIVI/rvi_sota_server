package org.genivi.sota.device_registry

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.testkit.ScalatestRouteTest
import eu.timepit.refined.api.Refined
import io.circe.Json
import org.genivi.sota.core.DatabaseSpec
import org.genivi.sota.data.GroupInfo.Name
import org.genivi.sota.data.{DeviceT, Uuid}
import org.genivi.sota.device_registry.common.CreateGroupRequest
import org.scalatest.{FunSuite, Matchers}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext

class GroupsResourceSpec extends FunSuite
    with ScalatestRouteTest
    with Matchers
    with DeviceRequests
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

  val complexJsonArray =
    Json.arr(Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromString("fish")))))
  val complexNumericJsonArray =
    Json.arr(Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromInt(5)))))
  val groupName: Name = Refined.unsafeApply("testGroup")

  test("creating groups is possible") {

    import org.genivi.sota.data.DeviceGenerators.genDeviceT

    val complexJsonObj = Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromString("fish"))))
    val complexNumericJsonObj = Json.fromFields(List(("key", Json.fromString("value")), ("type", Json.fromInt(5))))

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
    }

  }
}
