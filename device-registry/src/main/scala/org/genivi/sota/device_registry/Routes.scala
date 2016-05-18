/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.data.{Device, DeviceT}
import org.genivi.sota.data.Namespace._
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.device_registry.common.NamespaceDirective._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.rest.Validation._
import org.joda.time._
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


/**
 * API routes for device registry.
 *
 * @see {@linktourl http://pdxostc.github.io/rvi_sota_server/dev/api.html}
 */
class Routes(implicit system: ActorSystem,
             db: Database,
             mat: ActorMaterializer,
             ec: ExecutionContext) {

  import Device._
  import Directives._
  import Errors._
  import StatusCodes._

  val extractId: Directive1[Id] = refined[ValidId](Slash ~ Segment).map(Id(_))
  val extractDeviceId: Directive1[DeviceId] = parameter('deviceId.as[String]).map(DeviceId(_))


  def searchDevice(ns: Namespace): Route =
    parameters(('regex.as[String Refined Regex].?,
                'deviceName.as[String].?,
                'deviceId.as[String].?)) { (regex: Option[String Refined Regex],
                                            deviceName: Option[String],
                                            deviceId: Option[String]) =>
      (regex, deviceName, deviceId) match {
        case (Some(re), None, None) =>
          complete(db.run(Devices.search(ns, re)))
        case (None, Some(name), None) =>
          completeOrRecoverWith(db.run(Devices.findByDeviceName(ns, DeviceName(name)))) {
            onMissingDevice
          }
        case (None, None, Some(id)) =>
          completeOrRecoverWith(db.run(Devices.findByDeviceId(ns, DeviceId(id)))) {
            onMissingDevice
          }
        case (None, None, None) => complete(db.run(Devices.list))
        case _ =>
          complete((BadRequest, "'regex', 'deviceName' and 'deviceId' parameters cannot be used together!"))
      }
    }

  def createDevice(ns: Namespace, device: DeviceT): Route =
    complete(Created -> db.run(Devices.create(ns, device)))

  def fetchDevice(ns: Namespace, id: Id): Route =
    completeOrRecoverWith(db.run(Devices.exists(ns, id))) {
      onMissingDevice
    }

  def updateDevice(ns: Namespace, id: Id, device: DeviceT): Route =
    completeOrRecoverWith(db.run(Devices.update(ns, id, device))) {
      onMissingDevice.orElse(
        onConflictingDeviceId)
    }

  def deleteDevice(ns: Namespace, id: Id): Route =
    completeOrRecoverWith(db.run(Devices.delete(ns, id))) {
      onMissingDevice
    }

  def updateLastSeen(ns: Namespace, id: Id): Route =
    completeOrRecoverWith(db.run(Devices.updateLastSeen(ns, id))) {
      onMissingDevice
    }


  def api: Route =
    (pathPrefix("devices") & extractNamespace) { (ns: Namespace) =>
      (get & pathEnd) {
        searchDevice(ns)
      } ~
      (post & entity(as[DeviceT]) & pathEndOrSingleSlash) { (device: DeviceT) =>
        createDevice(ns, device)
      } ~
      extractId { (id: Id) =>
        (get & pathEnd) {
          fetchDevice(ns, id)
        } ~
        (put & entity(as[DeviceT]) & pathEnd) { (device: DeviceT) =>
          updateDevice(ns, id, device)
        } ~
        (delete & pathEnd) {
          deleteDevice(ns, id)
        } ~
        (post & path("ping")) {
          updateLastSeen(ns, id)
        }
      }
    }


  /**
   * Base API route for devices.
   *
   * @return      Route object containing routes for creating, deleting, and listing devices
   * @throws      MissingDevice if device doesn't exist
   */
  def route: Route = {
    api
  }

}
