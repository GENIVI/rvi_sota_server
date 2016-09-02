/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import cats.syntax.show._
import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpResponse, ResponseEntity, StatusCodes, Uri}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.data.{Device, DeviceT, Namespace, Uuid}
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

  val extractUuid: Directive1[Uuid] = refined[Uuid.Valid](Slash ~ Segment).map(Uuid(_))
  val extractDeviceId: Directive1[DeviceId] = parameter('deviceId.as[String]).map(DeviceId)

  def searchDevice(ns: Namespace): Route =
    parameters(('regex.as[String Refined Regex].?,
                'deviceId.as[String].?)) { // TODO: Use refined
      case (Some(re), None) =>
        complete(db.run(DeviceRepository.search(ns, re)))
      case (None, Some(deviceId)) =>
        complete(db.run(DeviceRepository.findByDeviceId(ns, DeviceId(deviceId))))
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

   onSuccess(f) { uuid =>
     respondWithHeaders(List(Location(Uri("/devices/" + uuid.show)))) {
        complete(Created -> uuid)
     }
   }
  }

  def fetchSystemInfo(uuid: Uuid): Route =
    complete(db.run(SystemInfo.findByUuid(uuid)))

  def createSystemInfo(uuid: Uuid, data: Json): Route =
    complete(Created -> db.run(SystemInfo.create(uuid, data)))

  def updateSystemInfo(uuid: Uuid, data: Json): Route =
    complete(db.run(SystemInfo.update(uuid, data)))

  def fetchGroupInfo(groupName: String, ns: Namespace): Route =
      complete(db.run(GroupInfo.findByName(groupName, ns)))

  def createGroupInfo(groupName: String, namespace: Namespace, data: Json): Route = {
    if (groupName.isEmpty) { complete(BadRequest -> "Group name cannot be empty") }
    else { complete(Created -> db.run(GroupInfo.create(groupName, namespace, data))) }
  }

  def updateGroupInfo(groupName: String, namespace: Namespace, data: Json): Route = {
    if (groupName.isEmpty) { complete(BadRequest -> "Group name cannot be empty") }
    else { complete(db.run(GroupInfo.update(groupName, namespace, data))) }
  }
  def deleteGroupInfo(groupName: String, namespace: Namespace): Route = {
    complete(db.run(GroupInfo.delete(groupName, namespace)))
  }

  def fetchDevice(uuid: Uuid): Route =
    complete(db.run(DeviceRepository.findByUuid(uuid)))

  def updateDevice(ns: Namespace, uuid: Uuid, device: DeviceT): Route =
    complete(db.run(DeviceRepository.update(ns, uuid, device)))

  def deleteDevice(ns: Namespace, uuid: Uuid): Route = {
    val f = db
      .run(DeviceRepository.delete(ns, uuid))
      .andThen {
        case scala.util.Success(_) =>
          messageBus.publish(DeviceDeleted(ns, uuid))
      }
    complete(f)
  }

  def updateLastSeen(uuid: Uuid): Route =
    complete(db.run(DeviceRepository.updateLastSeen(uuid)))

  implicit val NamespaceUnmarshaller: FromStringUnmarshaller[Namespace] = Unmarshaller.strict(Namespace.apply)

  def api: Route =
    ErrorHandler.handleErrors {
      pathPrefix("devices") {
        namespaceExtractor { ns =>
          (post & entity(as[DeviceT]) & pathEndOrSingleSlash) { device => createDevice(ns, device) } ~
          extractUuid { uuid =>
              (put & entity(as[DeviceT]) & pathEnd) { device =>
                updateDevice(ns, uuid, device)
              } ~
              (delete & pathEnd) {
                deleteDevice(ns, uuid)
              }
          } ~
          (get & path("group_info") & pathEnd & parameter('groupName.as[String])) {
            groupName => fetchGroupInfo(groupName, ns)
          } ~
          (post & path("group_info") & pathEnd & parameter('groupName.as[String])) { groupName =>
            entity(as[Json]) {body => createGroupInfo(groupName, ns, body)}
          } ~
          (put & path("group_info") & pathEnd & parameter('groupName.as[String])) { groupName =>
            entity(as[Json]) {body => updateGroupInfo(groupName, ns, body)}
          } ~
          (delete & path("group_info") & pathEnd & parameter('groupName.as[String])) { groupName =>
            deleteGroupInfo(groupName, ns)
          }
        } ~
        (extractUuid & post & path("ping")) { uuid =>
          updateLastSeen(uuid)
        } ~
        (extractUuid & get & path("system_info") & pathEnd) { uuid =>
          fetchSystemInfo(uuid)
        } ~
        (extractUuid & post & path("system_info") & pathEnd) { uuid =>
          entity(as[Json]) {body => createSystemInfo(uuid, body)}
        } ~
        (extractUuid & put & path("system_info") & pathEnd) { uuid =>
          entity(as[Json]) {body => updateSystemInfo(uuid, body)}
        } ~
        (extractUuid & get & pathEnd) { uuid =>
          fetchDevice(uuid)
        } ~
        (get & pathEnd & parameter('namespace.as[Namespace])) { ns =>
          searchDevice(ns)
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
