/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import akka.stream.ActorMaterializer
import cats.syntax.show._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.data._
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.device_registry.db._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.MessageBusPublisher._
import org.genivi.sota.messaging.Messages.{DeviceCreated, DeviceDeleted, DeviceSeen}
import org.genivi.sota.rest.Validation._
import org.genivi.sota.http.AuthDirectives.AuthScope
import org.genivi.sota.http.UuidDirectives.extractUuid
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

class DevicesResource(namespaceExtractor: Directive1[Namespace],
                      authDirective: AuthScope => Directive0,
                      messageBus: MessageBusPublisher,
                      deviceNamespaceAuthorizer: Directive1[Uuid])
                     (implicit system: ActorSystem,
             db: Database,
             mat: ActorMaterializer,
             ec: ExecutionContext) {

  import Device._
  import Directives._
  import StatusCodes._

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

  def getGroupsForDevice(uuid: Uuid): Route =
    complete(db.run(GroupMemberRepository.listGroupsForDevice(uuid)))


  def updateLastSeen(uuid: Uuid): Route = {
    val f = db.run(DeviceRepository.updateLastSeen(uuid)).pipeToBus(messageBus)(identity)
    complete(f)
  }

  implicit val NamespaceUnmarshaller: FromStringUnmarshaller[Namespace] = Unmarshaller.strict(Namespace.apply)

  def api: Route =
    pathPrefix("devices") {
      namespaceExtractor { ns =>
        (post & entity(as[DeviceT]) & pathEndOrSingleSlash) { device => createDevice(ns, device) } ~
        (get & pathEnd) { searchDevice(ns) } ~
        extractUuid { uuid =>
          //TODO: PRO-1666, use deviceNamespaceAuthorizer for this block, instead of extractUuid, once support has been
          // added into core
          (put & entity(as[DeviceT]) & pathEnd) { device =>
            updateDevice(ns, uuid, device)
          } ~
          (delete & pathEnd) {
            deleteDevice(ns, uuid)
          }
        }
      } ~
      extractUuid { uuid =>
        //TODO: PRO-1666, use deviceNamespaceAuthorizer once support has been added into core
        (post & path("ping")) {
          updateLastSeen(uuid)
        } ~
        (get & pathEnd) {
          fetchDevice(uuid)
        } ~
        (get & path("groups") & pathEnd) {
          getGroupsForDevice(uuid)
        }
      }
    }

  def mydeviceRoutes: Route =
    (pathPrefix("mydevice") & extractUuid) { uuid =>
      (post & path("ping") & authDirective(s"ota-core.${uuid.show}.write")) {
        updateLastSeen(uuid)
      } ~
      (get & pathEnd & authDirective(s"ota-core.${uuid.show}.read")) {
        fetchDevice(uuid)
      }
    }

  /**
   * Base API route for devices.
   *
   * @return      Route object containing routes for creating, deleting, and listing devices
   * @throws      Errors.MissingDevice if device doesn't exist
   */
  def route: Route = api ~ mydeviceRoutes

}
