package org.genivi.sota.device_registry

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri.Query
import org.genivi.sota.data.Group.Name
import org.genivi.sota.data.Uuid
import org.genivi.sota.marshalling.CirceMarshallingSupport._

import scala.concurrent.ExecutionContext


trait GroupRequests {
  self: ResourceSpec =>

  val groupsApi = "device_groups"

  def listDevicesInGroup(groupId: Uuid, offset: Option[Long] = None, limit: Option[Long] = None)
                        (implicit ec: ExecutionContext): HttpRequest =
    (offset, limit) match {
      case (None, None) =>
        Get(Resource.uri("device_groups", groupId.underlying.get, "devices"))
      case _ =>
        Get(Resource.uri("device_groups", groupId.underlying.get, "devices")
          .withQuery(Query("offset" -> offset.getOrElse(0).toString,
            "limit" -> limit.getOrElse(50).toString)))
    }

  def countDevicesInGroup(groupId: Uuid)(implicit ec: ExecutionContext): HttpRequest =
    Get(Resource.uri("device_groups", groupId.underlying.get, "count"))

  def listGroups(): HttpRequest = Get(Resource.uri(groupsApi))

  def createGroup(groupName: Name)
                     (implicit ec: ExecutionContext): HttpRequest =
    Post(Resource.uri(groupsApi).withQuery(Query("groupName" -> groupName.get)))

  def createGroupOk(groupName: Name)
                 (implicit ec: ExecutionContext): Uuid =
    createGroup(groupName) ~> route ~> check {
      status shouldBe Created
      responseAs[Uuid]
    }

  def addDeviceToGroup(groupId: Uuid, deviceId: Uuid)(implicit ec: ExecutionContext): HttpRequest =
    Post(Resource.uri(groupsApi, groupId.underlying.get, "devices", deviceId.underlying.get))

  def addDeviceToGroupOk(groupId: Uuid, deviceId: Uuid)(implicit ec: ExecutionContext): Unit =
    addDeviceToGroup(groupId, deviceId) ~> route ~> check {
      status shouldBe OK
    }

  def removeDeviceFromGroup(groupId: Uuid, deviceId: Uuid)(implicit ec: ExecutionContext): HttpRequest =
    Delete(Resource.uri(groupsApi, groupId.underlying.get, "devices", deviceId.underlying.get))

  def renameGroup(id: Uuid, newGroupName: Name)(implicit ec: ExecutionContext): HttpRequest = {
    Put(Resource.uri(groupsApi, id.underlying.get, "rename").withQuery(Query("groupName" -> newGroupName.get)))
  }
}
