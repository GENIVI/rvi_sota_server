/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import cats.syntax.show._
import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpResponse, MessageEntity, StatusCodes, Uri, ResponseEntity}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.data.{Device, DeviceT, Namespace}
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.rest.Validation._

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


/**
 * API routes for device registry.
 *
 * @see {@linktourl http://advancedtelematic.github.io/rvi_sota_server/dev/api.html}
 */
class Routes(namespaceExtractor: Directive1[Namespace])
            (implicit system: ActorSystem,
             db: Database,
             mat: ActorMaterializer,
             ec: ExecutionContext) {

  import Device._
  import Directives._
  import StatusCodes._

  val extractId: Directive1[Id] = refined[ValidId](Slash ~ Segment).map(Id(_))
  val extractDeviceId: Directive1[DeviceId] = parameter('deviceId.as[String]).map(DeviceId(_))

  def searchDevice(ns: Namespace): Route =
    parameters(('regex.as[String Refined Regex].?,
                'deviceId.as[String].?)) { // TODO: Use refined
      case (Some(re), None) =>
        complete(db.run(Devices.search(ns, re)))
      case (None, Some(id)) =>
        complete(db.run(Devices.findByDeviceId(ns, DeviceId(id))))
      case (None, None) => complete(db.run(Devices.list(ns)))
      case _ =>
        complete((BadRequest, "'regex' and 'deviceId' parameters cannot be used together!"))
    }

  def createDevice(ns: Namespace, device: DeviceT): Route = {
    val f = db.run(Devices.create(ns, device))
      .map(id => {
        Marshal(id).to[ResponseEntity].map { body =>
          HttpResponse(Created, List(Location(Uri("/devices/" + id.show))), body)
        }
      })
    complete(f)
  }

  def fetchDevice(id: Id): Route =
    complete(db.run(Devices.findById(id)))

  def updateDevice(ns: Namespace, id: Id, device: DeviceT): Route =
    complete(db.run(Devices.update(ns, id, device)))

  def deleteDevice(ns: Namespace, id: Id): Route =
    complete(db.run(Devices.delete(ns, id)))

  def updateLastSeen(id: Id): Route =
    complete(db.run(Devices.updateLastSeen(id)))

  implicit val NamespaceUnmarshaller: FromStringUnmarshaller[Namespace] = Unmarshaller.strict(Namespace.apply)

  def api: Route =
    handleExceptions(ExceptionHandler(Errors.onMissingDevice orElse Errors.onConflictingDevice)) {
      pathPrefix("devices") {
        namespaceExtractor { ns =>
          (post & entity(as[DeviceT]) & pathEndOrSingleSlash) { device => createDevice(ns, device) } ~
          extractId { id =>
              (put & entity(as[DeviceT]) & pathEnd) { device =>
                updateDevice(ns, id, device)
              } ~
              (delete & pathEnd) {
                deleteDevice(ns, id)
              }
          }
        } ~
        (extractId & post & path("ping")) { id =>
          updateLastSeen(id)
        } ~
        (extractId & get & pathEnd) { id =>
          fetchDevice(id)
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
