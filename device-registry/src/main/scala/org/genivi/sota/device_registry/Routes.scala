/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import cats.syntax.show._
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.data.{Device, DeviceT, Namespace}
import org.genivi.sota.device_registry.GroupInfo.Name
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.http.ErrorHandler
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.{DeviceCreated, DeviceDeleted}
import org.genivi.sota.rest.Validation._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._

class Routes(namespaceExtractor: Directive1[Namespace],
             messageBus: MessageBusPublisher)
            (implicit system: ActorSystem,
             db: Database,
             mat: ActorMaterializer,
             ec: ExecutionContext) {

  import Device._
  import Directives._
  import StatusCodes._

  val logger = LoggerFactory.getLogger(this.getClass)

  val extractId: Directive1[Id] = refined[ValidId](Slash ~ Segment).map(Id)
  val extractDeviceId: Directive1[DeviceId] = parameter('deviceId.as[String]).map(DeviceId)
  val extractGroupName: Directive1[GroupInfo.Name] =
    refined[GroupInfo.ValidName](Slash ~ Segment)

  def searchDevice(ns: Namespace): Route =
    parameters(('regex.as[String Refined Regex].?,
                'deviceId.as[String].?)) { // TODO: Use refined
      case (Some(re), None) =>
        complete(db.run(DeviceRepository.search(ns, re)))
      case (None, Some(id)) =>
        complete(db.run(DeviceRepository.findByDeviceId(ns, DeviceId(id))))
      case (None, None) => complete(db.run(DeviceRepository.list(ns)))
      case _ =>
        complete((BadRequest, "'regex' and 'deviceId' parameters cannot be used together!"))
    }

  def createDevice(ns: Namespace, device: DeviceT): Route = {
    val f = db
      .run(DeviceRepository.create(ns, device))
      .andThen {
        case scala.util.Success(uuid) =>
          messageBus.publish(DeviceCreated(ns, uuid, device.deviceName, device.deviceId, device.deviceType))
      }

   onSuccess(f) { id =>
     respondWithHeaders(List(Location(Uri("/devices/" + id.show)))) {
        complete(Created -> id)
     }
   }
  }

  def fetchSystemInfo(id: Id): Route =
    complete(db.run(SystemInfo.findById(id)))

  def createSystemInfo(id: Id, data: Json): Route =
    complete(Created -> db.run(SystemInfo.create(id, data)))

  def updateSystemInfo(id: Id, data: Json): Route =
    complete(db.run(SystemInfo.update(id, data)))

  def fetchGroupInfo(groupName: Name, ns: Namespace): Route =
      complete(db.run(GroupInfo.findByName(groupName, ns)))

  def createGroupInfo(groupName: Name, namespace: Namespace, data: Json): Route =
    complete(Created -> db.run(GroupInfo.create(groupName, namespace, data)))

  def updateGroupInfo(groupName: Name, namespace: Namespace, data: Json): Route =
    complete(db.run(GroupInfo.update(groupName, namespace, data)))

  def deleteGroupInfo(groupName: Name, namespace: Namespace): Route =
    complete(db.run(GroupInfo.delete(groupName, namespace)))

  def fetchDevice(id: Id): Route =
    complete(db.run(DeviceRepository.findById(id)))

  def updateDevice(ns: Namespace, id: Id, device: DeviceT): Route =
    complete(db.run(DeviceRepository.update(ns, id, device)))

  def deleteDevice(ns: Namespace, id: Id): Route = {
    val f = db
      .run(DeviceRepository.delete(ns, id))
      .andThen {
        case scala.util.Success(_) =>
          messageBus.publish(DeviceDeleted(ns, id))
      }
    complete(f)
  }

  def updateLastSeen(id: Id): Route =
    complete(db.run(DeviceRepository.updateLastSeen(id)))

  implicit val NamespaceUnmarshaller: FromStringUnmarshaller[Namespace] = Unmarshaller.strict(Namespace.apply)

  def api: Route =
    ErrorHandler.handleErrors {
      pathPrefix("devices") {
          (extractId & post & path("ping")) { id =>
            updateLastSeen(id)
          } ~
          (extractId & get & path("system_info") & pathEnd) { id =>
            fetchSystemInfo(id)
          } ~
          (extractId & post & path("system_info") & pathEnd) { id =>
            entity(as[Json]) {body => createSystemInfo(id, body)}
          } ~
          (extractId & put & path("system_info") & pathEnd) { id =>
            entity(as[Json]) {body => updateSystemInfo(id, body)}
          } ~
          (extractId & get & pathEnd) { id =>
            fetchDevice(id)
          } ~
          (get & pathEnd & parameter('namespace.as[Namespace])) { ns =>
            searchDevice(ns)
          } ~ {
            namespaceExtractor { ns =>
              (post & entity(as[DeviceT]) & pathEndOrSingleSlash) { device => createDevice(ns, device) } ~
                extractId { id =>
                  (put & entity(as[DeviceT]) & pathEnd) { device =>
                    updateDevice(ns, id, device)
                  } ~
                    (delete & pathEnd) {
                      deleteDevice(ns, id)
                    }
                } ~
                extractGroupName { groupName =>
                  (get & path("group_info")) {
                    fetchGroupInfo(groupName, ns)
                  } ~
                  (post & path("group_info")) {
                    entity(as[Json]) { body => createGroupInfo(groupName, ns, body) }
                  } ~
                  (put & path("group_info")) {
                    entity(as[Json]) { body => updateGroupInfo(groupName, ns, body) }
                  } ~
                  (delete & path("group_info")) {
                    deleteGroupInfo(groupName, ns)
                  }
                }
            }
          }
      }
    }

  /**
   * Base API route for devices.
   *
   * @return      Route object containing routes for creating, deleting, and listing devices
   * @throws      Errors.MissingDevice if device doesn't exist
   */
  def route: Route = api

}
