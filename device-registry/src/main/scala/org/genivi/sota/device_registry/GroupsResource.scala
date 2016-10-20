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
import org.genivi.sota.http.UuidDirectives.{allowExtractor, extractUuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import slick.driver.MySQLDriver.api._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class GroupsResource(namespaceExtractor: Directive1[Namespace], deviceNamespaceAuthorizer: Directive1[Uuid])
                    (implicit ec: ExecutionContext, db: Database)
  extends Directives {

  import SystemInfoRepository._
  import JsonMatcher._

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

  private def groupAllowed(groupId: Uuid): Future[Namespace] = {
    db.run(GroupInfoRepository.groupInfoNamespace(groupId))
  }

  private def deviceAllowed(deviceId: Uuid): Future[Namespace] = {
    db.run(DeviceRepository.deviceNamespace(deviceId))
  }

  def createGroupFromDevices(request: CreateGroupRequest, namespace: Namespace): Route = {
    complete(UpdateMemberships.createGroupFromDevices(request, namespace))
  }

  def getDevicesInGroup(groupId: Uuid): Route = {
    complete(db.run(GroupMemberRepository.listDevicesInGroup(groupId)))
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
    pathPrefix("device_groups") {
      namespaceExtractor { ns =>
        (post & path("from_attributes") & extractCreateGroupRequest) { request =>
          createGroupFromDevices(request, ns)
        } ~
        (post & extractUuid & pathEnd & parameter('groupName.as[Name])) { (groupId, groupName) =>
          entity(as[Json]) { body => createGroupInfo(groupId, groupName, ns, body) }
        } ~
        (get & pathEnd) {
          listGroups(ns)
        }
      } ~
      (get & extractUuid & path("devices") ) { groupId =>
        //TODO: PRO-1666, move this back into the extractGroupId block once core supports namespaces
        getDevicesInGroup(groupId)
      } ~
      extractUuid { groupId =>
        //TODO: PRO-1666, use extractGroupId/deviceNamespaceAuthorizer once support has been added to core
        (post & pathPrefix("devices") & extractUuid) { deviceId =>
          addDeviceToGroup(groupId, deviceId)
        } ~
        (delete & pathPrefix("devices") & extractUuid) { deviceId =>
          removeDeviceFromGroup(groupId, deviceId)
        } ~
        (put & path("rename") & parameter('groupName.as[Name])) { groupName =>
          renameGroup(groupId, groupName)
        } ~
        (get & pathEnd) {
          fetchGroupInfo(groupId)
        } ~
        (put & parameter('groupName.as[Name])) { groupName =>
          entity(as[Json]) { body => updateGroupInfo(groupId, groupName, body) }
        } ~
        (get & path("count") & pathEnd) {
          countDevices(groupId)
        } ~
        (get & path("discarded_attrs") & pathEnd) {
          getDiscardedAttrs(groupId)
        }
      }
    }
}
