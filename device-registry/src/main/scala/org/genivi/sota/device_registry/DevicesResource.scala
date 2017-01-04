/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import java.time.{Instant, OffsetDateTime}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import cats.syntax.show._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.data._
import org.genivi.sota.device_registry.db._
import org.genivi.sota.http.UuidDirectives.extractUuid
import org.genivi.sota.http.{AuthedNamespaceScope, Scopes}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.{DeviceActivated, DeviceCreated, DeviceDeleted, DeviceSeen}
import org.genivi.sota.rest.Validation._
import org.genivi.sota.unmarshalling.AkkaHttpUnmarshallingSupport._
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

class DevicesResource(namespaceExtractor: Directive1[AuthedNamespaceScope],
                      messageBus: MessageBusPublisher,
                      deviceNamespaceAuthorizer: Directive1[Uuid])
                     (implicit system: ActorSystem,
             db: Database,
             mat: ActorMaterializer,
             ec: ExecutionContext) {

  import Device._
  import Directives._
  import StatusCodes._

  val extractPackageId: Directive1[PackageId] = (refined[PackageId.ValidName](Slash ~ Segment)
                                                & refined[PackageId.ValidVersion](Slash ~ Segment))
                                                .as(PackageId.apply _)

  def searchDevice(ns: Namespace): Route =
    parameters(('regex.as[String Refined Regex].?,
                'deviceId.as[String].?, // TODO: Use refined
                'offset.as[Long].?,
                'limit.as[Long].?)) {
      case (Some(re), None, offset, limit) =>
        complete(db.run(DeviceRepository.search(ns, re, offset, limit)))
      case (None, Some(deviceId), offset, limit) =>
        complete(db.run(DeviceRepository.findByDeviceId(ns, DeviceId(deviceId))))
      case (None, None, offset, limit) =>
        complete(db.run(DeviceRepository.list(ns, offset, limit)))
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
    val now = Instant.now()

    val f = db.run(DeviceRepository.updateLastSeen(uuid, now))
      .andThen {
        case scala.util.Success((activated, ns)) =>
          messageBus.publish(DeviceSeen(ns, uuid, now))
          if (activated) messageBus.publish(DeviceActivated(ns, uuid, now))
      }

    complete(f)
  }

  def updateInstalledSoftware(device: Uuid): Route = {
    entity(as[Seq[PackageId]]) { installedSoftware =>
      val f = db.run(InstalledPackages.setInstalled(device, installedSoftware.toSet))
      onSuccess(f) { complete(StatusCodes.NoContent) }
    }
  }

  def updateInstalledSoftwareForDevices(): Route = {
    entity(as[Seq[(Uuid, Seq[PackageId])]]) { installedDeviceSoftware =>
      val f = db.run(InstalledPackages.setInstalledForDevices(installedDeviceSoftware))
      onSuccess(f) { complete(StatusCodes.NoContent) }
    }
  }

  def getDevicesCount(pkg: PackageId, ns: Namespace): Route =
    complete(db.run(InstalledPackages.getDevicesCount(pkg, ns)))

  def listPackagesOnDevice(device: Uuid): Route =
    complete(db.run(InstalledPackages.installedOn(device)))

  def getActiveDeviceCount(ns: Namespace): Route =
    parameters(('start.as[OffsetDateTime], 'end.as[OffsetDateTime])) { (start, end) =>
      complete(db.run(DeviceRepository.countActivatedDevices(ns, start.toInstant, end.toInstant)))
    }

  def api: Route = namespaceExtractor { ns =>
    val scope = Scopes.devices(ns)
    pathPrefix("devices") {
      (scope.post & entity(as[DeviceT]) & pathEndOrSingleSlash) { device => createDevice(ns, device) } ~
      (scope.get & pathEnd) { searchDevice(ns) } ~
      deviceNamespaceAuthorizer { uuid =>
        (scope.put & entity(as[DeviceT]) & pathEnd) { device =>
          updateDevice(ns, uuid, device)
        } ~
        (scope.delete & pathEnd) {
          deleteDevice(ns, uuid)
        } ~
        (scope.post & path("ping")) {
          updateLastSeen(uuid)
        } ~
        (scope.get & pathEnd) {
          fetchDevice(uuid)
        } ~
        (scope.get & path("groups") & pathEnd) {
          getGroupsForDevice(uuid)
        } ~
        (path("packages") & scope.get) {
          listPackagesOnDevice(uuid)
        }
      }
    } ~
    (scope.get & pathPrefix("device_count") & extractPackageId) { pkg =>
      getDevicesCount(pkg, ns)
    } ~
    (scope.get & path("active_device_count")) {
      getActiveDeviceCount(ns)
    }
  }

  def mydeviceRoutes: Route = namespaceExtractor { authedNs => // Don't use this as a namespace
    (pathPrefix("mydevice") & extractUuid) { uuid =>
      (post & path("ping") & authedNs.oauthScope(s"ota-core.${uuid.show}.write")) {
        updateLastSeen(uuid)
      } ~
      (get & pathEnd & authedNs.oauthScopeReadonly(s"ota-core.${uuid.show}.read")) {
        fetchDevice(uuid)
      } ~
      (put & path("packages") & authedNs.oauthScope(s"ota-core.${uuid.show}.write")) {
        updateInstalledSoftware(uuid)
      }
    }
  }

  /**
   * Base API route for devices.
   *
   * @return      Route object containing routes for creating, deleting, and listing devices
   * @throws      Errors.MissingDevice if device doesn't exist
   */
  def route: Route = api ~ mydeviceRoutes ~
    (pathPrefix("mydevice") & put & path("packages")) {
      updateInstalledSoftwareForDevices()
    }

}
