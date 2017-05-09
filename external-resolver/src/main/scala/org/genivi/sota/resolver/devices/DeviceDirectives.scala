/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.devices

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.{Namespace, PackageId, Uuid}
import org.genivi.sota.device_registry.common.{Errors => DeviceRegistryErrors}
import org.genivi.sota.http.AuthedNamespaceScope
import org.genivi.sota.http.ErrorHandler
import org.genivi.sota.http.Scopes
import org.genivi.sota.http.UuidDirectives.extractUuid
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.InstalledSoftware
import org.genivi.sota.resolver.common.RefinementDirectives.{refinedPackageId, refinedPartNumber}
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.db.{DeviceRepository, ForeignPackages}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * API routes for everything related to vehicles: creation, deletion, and package and component association.
 *
 * @see {@linktourl http://advancedtelematic.github.io/rvi_sota_server/dev/api.html}
 */
class DeviceDirectives(namespaceExtractor: Directive1[AuthedNamespaceScope],
                       deviceRegistry: DeviceRegistry)
                      (implicit system: ActorSystem,
                        db: Database,
                        mat: ActorMaterializer,
                        ec: ExecutionContext) {

  import Directives._

  def extractDeviceUuid(ns: Namespace): Directive1[Uuid] = extractUuid.flatMap { deviceId =>
    onComplete(deviceRegistry.fetchDevice(ns, deviceId)).flatMap {
      case Success(_) => provide(deviceId)
      case Failure(DeviceRegistryErrors.MissingDevice) => complete(DeviceRegistryErrors.MissingDevice)
      case Failure(t) => reject(AuthorizationFailedRejection)
    }
  }

  def searchDevices(ns: Namespace): Route =
    parameters(('regex.as[String Refined Regex].?,
      'packageName.as[PackageId.Name].?,
      'packageVersion.as[PackageId.Version].?,
      'component.as[Component.PartNumber].?)) { case (re, pn, pv, cp) =>
      complete(DeviceRepository.search(ns, re, pn, pv, cp, deviceRegistry))
    }

  def getPackages(device: Uuid, regexFilter: Option[Refined[String, Regex]]): Route = {
    val result = for {
      native <- DeviceRepository.installedOn(device, regexFilter.map(_.get)).map(_.map(_.id))
      foreign <- ForeignPackages.installedOn(device, regexFilter.map(_.get))
    } yield native ++ foreign

    complete(db.run(result))
  }


  def installPackage(namespace: Namespace, device: Uuid, pkgId: PackageId): Route =
    complete(db.run(DeviceRepository.installPackage(namespace, device, pkgId)))

  def uninstallPackage(ns: Namespace, device: Uuid, pkgId: PackageId): Route =
    complete(db.run(DeviceRepository.uninstallPackage(ns, device, pkgId)))

  def updateInstalledSoftware(device: Uuid): Route = {
    def updateSoftwareOnDb(namespace: Namespace, installedSoftware: InstalledSoftware): Future[Unit] = {
      db.run {
        for {
          _ <- DeviceRepository.updateInstalledPackages(namespace, device, installedSoftware.packages)
          _ <- DeviceRepository.updateInstalledFirmware(device, installedSoftware.firmware)
          _ <- ForeignPackages.setInstalled(device, installedSoftware.packages)
        } yield ()
      }
    }

    entity(as[InstalledSoftware]) { installedSoftware =>
      val responseF = {
        for {
          deviceData <- deviceRegistry.fetchMyDevice(device)
          _ <- updateSoftwareOnDb(deviceData.namespace, installedSoftware)
          _ <- deviceRegistry.setInstalledPackages(device, installedSoftware.packages.toSeq)
        } yield ()
      }

      onSuccess(responseF) { complete(StatusCodes.NoContent) }
    }
  }

  /**
   * API route for package -> vehicle associations.
   *
   * @return      Route object containing routes for listing packages on a vehicle, and creating and deleting
   *              vehicle -> package associations
   * @throws      Errors.MissingPackageException if package doesn't exist
    */
  def packageApi(device: Uuid): Route = {
    (pathPrefix("package") & namespaceExtractor) { ns =>
      val scope = Scopes.devices(ns)
      (scope.get & pathEnd) {
        parameters('regex.as[Refined[String, Regex]].?) { regFilter =>
          getPackages(device, regFilter)
        }
      } ~
      refinedPackageId { pkgId =>
        (scope.put & pathEnd) {
          installPackage(ns, device, pkgId)
        } ~
        (scope.delete & pathEnd) {
          uninstallPackage(ns, device, pkgId)
        }
      }
    }
  }

  def packagesApi: Route = namespaceExtractor { authedNs =>
    extractUuid { device =>
      (path("packages") & put & authedNs.oauthScope(s"ota-core.{device.show}.write")) {
        updateInstalledSoftware(device)
      }
    }
  }

  def getComponents(ns: Namespace, device: Uuid): Route =
    complete(db.run(DeviceRepository.componentsOnDevice(ns, device)))

  def installComponent(ns: Namespace, device: Uuid, part: Component.PartNumber): Route =
    complete(db.run(DeviceRepository.installComponent(ns, device, part)))

  def uninstallComponent(ns: Namespace, device: Uuid, part: Component.PartNumber): Route =
    complete(db.run(DeviceRepository.uninstallComponent(ns, device, part)))

  /**
   * API route for component -> vehicle associations.
   *
   * @return      Route object containing routes for listing components on a vehicle, and creating and deleting
   *              vehicle -> component associations
   * @throws      Errors.MissingComponent if component doesn't exist
   */
  def componentApi(device: Uuid): Route =
    (pathPrefix("component") & namespaceExtractor) { ns =>
      val scope = Scopes.devices(ns)
      (scope.get & pathEnd) {
        getComponents(ns, device)
      } ~
      refinedPartNumber { part =>
        (scope.put & pathEnd) {
          installComponent(ns, device, part)
        } ~
        (scope.delete & pathEnd) {
          uninstallComponent(ns, device, part)
        }
      }
    }

  def deviceApi: Route =
    pathPrefix("devices") {
      namespaceExtractor { ns =>
        val scope = Scopes.devices(ns)
        (scope.get & pathEnd) { searchDevices(ns) } ~
        extractDeviceUuid(ns) { device =>
          packageApi(device) ~
          componentApi(device)
        } ~
        packagesApi
      }
    }

  def getFirmware(ns: Namespace, deviceId: Uuid): Route =
    complete(db.run(DeviceRepository.firmwareOnDevice(ns, deviceId)))

  def route: Route = ErrorHandler.handleErrors {
    deviceApi ~
    (pathPrefix("firmware") & get & namespaceExtractor & extractUuid) { (ns, device) =>
      Scopes.devices(ns).checkReadonly {
        getFirmware(ns, device)
      }
    }
  }
}
