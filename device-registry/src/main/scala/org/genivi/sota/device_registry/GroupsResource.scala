package org.genivi.sota.device_registry

import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.data.GroupInfo.Name
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.device_registry.common.CreateGroupRequest
import org.genivi.sota.device_registry.db._
import org.genivi.sota.http.{AuthedNamespaceScope, Scopes}
import org.genivi.sota.http.UuidDirectives.{allowExtractor, extractUuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.slf4j.LoggerFactory
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class GroupsResource(namespaceExtractor: Directive1[AuthedNamespaceScope], deviceNamespaceAuthorizer: Directive1[Uuid])
                    (implicit ec: ExecutionContext, db: Database)
  extends Directives {

  private val extractGroupId = allowExtractor(namespaceExtractor, extractUuid, groupAllowed)

  private val extractCreateGroupRequest: Directive1[CreateGroupRequest] =
    (namespaceExtractor & entity(as[CreateGroupRequest])).tflatMap { case (ns, request) =>
      val devicesExist = for {
        _ <- DeviceRepository.exists(ns, request.device1)
        _ <- DeviceRepository.exists(ns, request.device2)
      } yield ()
      onComplete(db.run(devicesExist)).flatMap {
        case Success(_) => provide(request)
        case Failure(error) => reject(ValidationRejection("Device not found"))
      }
    }

  private val logger = LoggerFactory.getLogger(this.getClass)

  private def groupAllowed(groupId: Uuid): Future[Namespace] =
    db.run(GroupInfoRepository.groupInfoNamespace(groupId))

  private def deviceAllowed(deviceId: Uuid): Future[Namespace] =
    db.run(DeviceRepository.deviceNamespace(deviceId))

  def createGroupFromDevices(request: CreateGroupRequest, namespace: Namespace): Route =
    complete(UpdateMemberships.createGroupFromDevices(request, namespace))

  def getDevicesInGroup(groupId: Uuid): Route = {
    parameters(('offset.as[Long].?, 'limit.as[Long].?)) { (offset, limit) =>
      complete(db.run(GroupMemberRepository.listDevicesInGroup(groupId, offset, limit)))
    }
  }

  def listGroups(ns: Namespace): Route =
    complete(db.run(GroupInfoRepository.list(ns)))

  def fetchGroupInfo(groupId: Uuid): Route =
    complete(db.run(GroupInfoRepository.getGroupInfoById(groupId)))

  def createGroupInfo(id: Uuid,
                      groupName: Name,
                      namespace: Namespace,
                      groupInfo: Json): Route =
    complete(StatusCodes.Created -> db.run(GroupInfoRepository.create(id, groupName, namespace, groupInfo, Json.Null)))

  def updateGroupInfo(groupId: Uuid, groupName: Name, groupInfo: Json): Route =
    complete(db.run(GroupInfoRepository.updateGroupInfo(groupId, groupInfo)))

  def renameGroup(groupId: Uuid, newGroupName: Name): Route =
    complete(db.run(GroupInfoRepository.renameGroup(groupId, newGroupName)))

  def countDevices(groupId: Uuid): Route =
    complete(db.run(GroupMemberRepository.countDevicesInGroup(groupId)))

  def addDeviceToGroup(groupId: Uuid, deviceId: Uuid): Route =
    complete(db.run(GroupMemberRepository.addGroupMember(groupId, deviceId)))

  def removeDeviceFromGroup(groupId: Uuid, deviceId: Uuid): Route =
    complete(db.run(GroupMemberRepository.removeGroupMember(groupId, deviceId)))

  def getDiscardedAttrs(groupId: Uuid): Route =
    complete(db.run(GroupInfoRepository.discardedAttrs(groupId)))

  val route: Route =
    (pathPrefix("device_groups") & namespaceExtractor) { ns =>
      val scope = Scopes.devices(ns)
      (scope.post & path("from_attributes") & extractCreateGroupRequest) { request =>
        createGroupFromDevices(request, ns)
      } ~
      parameter('groupName.as[Name]) { groupName =>
        (scope.post & path("group_info") & entity(as[Json])) { body =>
          createGroupInfo(Uuid.generate(), groupName, ns, body)
        } ~
        (scope.post & pathEnd) { createGroupInfo(Uuid.generate(), groupName, ns, Json.Null) }
      } ~
      (scope.get & pathEnd) {
        listGroups(ns)
      } ~
      (scope.get & extractGroupId & path("devices")) { groupId =>
        getDevicesInGroup(groupId)
      } ~
      extractGroupId { groupId =>
        (scope.post & pathPrefix("devices") & deviceNamespaceAuthorizer) { deviceId =>
          addDeviceToGroup(groupId, deviceId)
        } ~
        (scope.delete & pathPrefix("devices") & deviceNamespaceAuthorizer) { deviceId =>
          removeDeviceFromGroup(groupId, deviceId)
        } ~
        (scope.put & path("rename") & parameter('groupName.as[Name])) { groupName =>
          renameGroup(groupId, groupName)
        } ~
        (scope.get & pathEnd) {
          fetchGroupInfo(groupId)
        } ~
        (scope.put & pathPrefix("group_info") & parameter('groupName.as[Name])) { groupName =>
          entity(as[Json]) { body => updateGroupInfo(groupId, groupName, body) }
        } ~
        (scope.get & path("count") & pathEnd) {
          countDevices(groupId)
        } ~
        (scope.get & path("discarded_attrs") & pathEnd) {
          getDiscardedAttrs(groupId)
        }
      }
    }
}
