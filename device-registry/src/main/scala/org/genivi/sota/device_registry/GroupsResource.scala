package org.genivi.sota.device_registry

import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.data.GroupInfo.Name
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.device_registry.common.CreateGroupRequest
import org.genivi.sota.http.UuidDirectives.{extractUuid, allowExtractor}
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import scala.concurrent.{ExecutionContext, Future}
import slick.driver.MySQLDriver.api._


class GroupsResource(namespaceExtractor: Directive1[Namespace])
                    (implicit ec: ExecutionContext,
                    db: Database)
  extends Directives {

  import org.genivi.sota.marshalling.CirceMarshallingSupport._
  import StatusCodes.Created

  private val extractGroupId = allowExtractor(namespaceExtractor, extractUuid, deviceAllowed)

  private def deviceAllowed(groupId: Uuid): Future[Namespace] = {
    db.run(GroupInfoRepository.groupInfoNamespace(groupId))
  }

  def createGroupFromDevices(request: CreateGroupRequest, namespace: Namespace): Route = {
    val dbIo = for {
      info1 <- SystemInfo.findByUuid(request.device1)
      info2 <- SystemInfo.findByUuid(request.device2)
    } yield JsonComparison.getCommonJson(info1, info2)

    onSuccess(db.run(dbIo)){
        case Json.Null =>
          complete(StatusCodes.BadRequest -> "Devices have no common attributes to form a group")
        case json =>
          val groupId = Uuid.generate()
          val dbIO = for {
            _       <- GroupInfoRepository.create(groupId, request.groupName, namespace, json)
            _       <- GroupMember.createGroup(groupId, namespace, request.device1)
            _       <- GroupMember.createGroup(groupId, namespace, request.device2)
                 //PRO-1378 Add logic to find devices which should be in this group
          } yield groupId
          complete(db.run(dbIO.transactionally))
    }
  }

  def getDevicesInGroup(groupId: Uuid): Route = {
    complete(db.run(GroupMember.listDevicesInGroup(groupId)))
  }

  def listGroups(ns: Namespace): Route =
    complete(db.run(GroupInfoRepository.list(ns)))

  def fetchGroupInfo(groupId: Uuid): Route =
    complete(db.run(GroupInfoRepository.getGroupInfoById(groupId)))

  def createGroupInfo(id: Uuid, groupName: Name, namespace: Namespace, data: Json): Route =
    complete(Created -> db.run(GroupInfoRepository.create(id, groupName, namespace, data)))

  def updateGroupInfo(groupId: Uuid, groupName: Name, data: Json): Route =
    complete(db.run(GroupInfoRepository.updateGroupInfo(groupId, data)))

  def renameGroup(groupId: Uuid, newGroupName: Name): Route =
    complete(db.run(GroupInfoRepository.renameGroup(groupId, newGroupName)))

  def countDevices(groupId: Uuid): Route =
    complete(db.run(GroupMember.countDevicesInGroup(groupId)))

  val route: Route =
    pathPrefix("device_groups") {
      namespaceExtractor { ns =>
        (post & path("from_attributes")) {
          entity(as[CreateGroupRequest]) { groupInfo => createGroupFromDevices(groupInfo, ns) }
        } ~
        (post & extractUuid & pathEnd & parameter('groupName.as[Name])) { (groupId, groupName) =>
          entity(as[Json]) { body => createGroupInfo(groupId, groupName, ns, body) }
        } ~
        (get & pathEnd) {
          listGroups(ns)
        }
      } ~
      extractGroupId { groupId =>
        (get & path("devices")) {
          getDevicesInGroup(groupId)
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
        }
      }
    }

}
