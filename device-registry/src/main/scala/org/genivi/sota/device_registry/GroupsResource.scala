package org.genivi.sota.device_registry

import slick.driver.MySQLDriver.api._
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.device_registry.common.CreateGroupRequest
import org.genivi.sota.http.UuidDirectives.extractUuid

import scala.concurrent.ExecutionContext

class GroupsResource(namespaceExtractor: Directive1[Namespace])
                    (implicit ec: ExecutionContext,
                    db: Database)
  extends Directives {

  import org.genivi.sota.marshalling.CirceMarshallingSupport._

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

  val route: Route =
    (pathPrefix("device_groups") & namespaceExtractor) { ns =>
      (post & path("from_attributes")) {
        entity(as[CreateGroupRequest]) { groupInfo => createGroupFromDevices(groupInfo, ns) }
      } ~
      (get & extractUuid & pathPrefix("devices") & pathEnd) { groupId =>
        getDevicesInGroup(groupId)
      }
    }

}
