package org.genivi.sota.device_registry

import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import io.circe.generic.auto._
import org.genivi.sota.data.Group.Name
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.device_registry.db._
import org.genivi.sota.http.{AuthedNamespaceScope, Scopes}
import org.genivi.sota.http.UuidDirectives.{allowExtractor, extractUuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class GroupsResource(namespaceExtractor: Directive1[AuthedNamespaceScope], deviceNamespaceAuthorizer: Directive1[Uuid])
                    (implicit ec: ExecutionContext, db: Database)
  extends Directives {

  private val extractGroupId = allowExtractor(namespaceExtractor, extractUuid, groupAllowed)

  private def groupAllowed(groupId: Uuid): Future[Namespace] =
    db.run(GroupInfoRepository.groupInfoNamespace(groupId))

  def getDevicesInGroup(groupId: Uuid): Route = {
    parameters(('offset.as[Long].?, 'limit.as[Long].?)) { (offset, limit) =>
      complete(db.run(GroupMemberRepository.listDevicesInGroup(groupId, offset, limit)))
    }
  }

  def listGroups(ns: Namespace): Route =
    complete(db.run(GroupInfoRepository.list(ns)))

  def createGroup(id: Uuid,
                  groupName: Name,
                  namespace: Namespace): Route =
    complete(StatusCodes.Created -> db.run(GroupInfoRepository.create(id, groupName, namespace)))

  def renameGroup(groupId: Uuid, newGroupName: Name): Route =
    complete(db.run(GroupInfoRepository.renameGroup(groupId, newGroupName)))

  def countDevices(groupId: Uuid): Route =
    complete(db.run(GroupMemberRepository.countDevicesInGroup(groupId)))

  def addDeviceToGroup(groupId: Uuid, deviceId: Uuid): Route =
    complete(db.run(GroupMemberRepository.addGroupMember(groupId, deviceId)))

  def removeDeviceFromGroup(groupId: Uuid, deviceId: Uuid): Route =
    complete(db.run(GroupMemberRepository.removeGroupMember(groupId, deviceId)))

  val route: Route =
    (pathPrefix("device_groups") & namespaceExtractor) { ns =>
      val scope = Scopes.devices(ns)
      (scope.post & parameter('groupName.as[Name]) & pathEnd) { groupName =>
        createGroup(Uuid.generate(), groupName, ns)
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
        (scope.get & path("count") & pathEnd) {
          countDevices(groupId)
        }
      }
    }
}
