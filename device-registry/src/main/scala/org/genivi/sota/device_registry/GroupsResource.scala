package org.genivi.sota.device_registry

import slick.driver.MySQLDriver.api._
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.data.Namespace
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.device_registry.common.CreateGroupRequest

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
          val dbIO = for {
            _ <- GroupInfoRepository.create(request.groupName, namespace, json)
            _ <- GroupMember.createGroup(request, namespace)
                 //PRO-1378 Add logic to find devices which should be in this group
          } yield ()
          complete(db.run(dbIO.transactionally))
    }
  }

  val route: Route =
    (pathPrefix("device_groups") & namespaceExtractor) { ns =>
      (post & pathPrefix("from_attributes") & pathEnd) {
        entity(as[CreateGroupRequest]) { groupInfo => createGroupFromDevices(groupInfo, ns) }
      }
    }

}
