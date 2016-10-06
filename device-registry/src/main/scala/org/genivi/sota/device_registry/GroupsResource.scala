package org.genivi.sota.device_registry

import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.http.scaladsl.util.FastFuture
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.data.GroupInfo.Name
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.device_registry.common.CreateGroupRequest
import org.genivi.sota.http.UuidDirectives.{extractUuid, allowExtractor}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import scala.concurrent.{ExecutionContext, Future}
import slick.driver.MySQLDriver.api._


class GroupsResource(namespaceExtractor: Directive1[Namespace])
                    (implicit ec: ExecutionContext, db: Database)
  extends Directives {

  import SystemInfo._

  private val extractGroupId = allowExtractor(namespaceExtractor, extractUuid, deviceAllowed)

  private def deviceAllowed(groupId: Uuid): Future[Namespace] = {
    db.run(GroupInfoRepository.groupInfoNamespace(groupId))
  }

  def matchDevices(ns: Namespace, groupId: Uuid, groupInfo: Json): Future[Int] =
    db.run(list(ns)).flatMap { infos =>
      val fs = infos.map { info =>
        JsonComparison.getCommonJson(info.systemInfo, groupInfo) match {
          case Json.Null => Future.successful(0)
          case json if json == groupInfo =>
            db.run(GroupMember.createGroup(groupId, ns, info.uuid))
          case _ => Future.successful(0)
        }
      }
      Future.sequence(fs).map(_.sum)
    }

  def createGroupFromDevices(request: CreateGroupRequest, namespace: Namespace): Route = {
    val dbIo = for {
      info1 <- findByUuid(request.device1)
      info2 <- findByUuid(request.device2)
    } yield JsonComparison.getCommonJson(info1, info2)

    onSuccess(db.run(dbIo)) {
      case Json.Null =>
        complete(BadRequest -> "Devices have no common attributes to form a group")
      case json =>
        val groupId = Uuid.generate()
        val dbIO = for {
          _ <- GroupInfoRepository.create(groupId, request.groupName, namespace, json)
          _ <- GroupMember.createGroup(groupId, namespace, request.device1)
          _ <- GroupMember.createGroup(groupId, namespace, request.device2)
        } yield groupId
        val f = db.run(dbIO.transactionally)
        f.onSuccess { case gid =>
          matchDevices(namespace, gid, json)
        }
        complete(f)
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
