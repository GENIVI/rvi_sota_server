/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.vehicles

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server._
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.data.Firmware
import org.genivi.sota.resolver.common.InstalledSoftware
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.common.NamespaceDirective._
import org.genivi.sota.resolver.common.RefinementDirectives.{refinedPackageId, refinedPartNumber}
import org.genivi.sota.resolver.components.{Component, ComponentRepository}
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.rest.Validation._
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import scala.concurrent.ExecutionContext
import slick.dbio.{DBIOAction, DBIO}
import slick.driver.MySQLDriver.api._


/**
 * API routes for everything related to vehicles: creation, deletion, and package and component association.
 *
 * @see {@linktourl http://pdxostc.github.io/rvi_sota_server/dev/api.html}
 */
class VehicleDirectives(implicit system: ActorSystem,
                        db: Database,
                        mat: ActorMaterializer,
                        ec: ExecutionContext) {
  import Directives._

  /**
   * Exception handler for package routes.
   */
  def installedPackagesHandler: ExceptionHandler =
    ExceptionHandler(Errors.onMissingPackage orElse Errors.onMissingVehicle)

  /**
   * Exception handler for component routes.
   */
  def installedComponentsHandler: ExceptionHandler =
    ExceptionHandler(Errors.onMissingVehicle orElse Errors.onMissingComponent)

  val extractVin : Directive1[Vehicle.Vin] = refined[Vehicle.ValidVin](Slash ~ Segment)

  def searchVehicles(ns: Namespace): Route =
    parameters(('regex.as[String Refined Regex].?,
                'packageName.as[PackageId.Name].?,
                'packageVersion.as[PackageId.Version].?,
                'component.as[Component.PartNumber].?)) { case (re, pn, pv, cp) =>
      complete(db.run(VehicleRepository.search(ns, re, pn, pv, cp)))
    }

  def getVehicle(ns: Namespace, vin: Vehicle.Vin): Route =
    completeOrRecoverWith(db.run(VehicleRepository.exists(ns, vin))) {
      Errors.onMissingVehicle
    }

  def addVehicle(ns: Namespace, vin: Vehicle.Vin): Route =
    complete(db.run(VehicleRepository.add(Vehicle(ns, vin))).map(_ => NoContent))

  def deleteVehicle(ns: Namespace, vin: Vehicle.Vin): Route =
    handleExceptions(installedPackagesHandler) {
      complete(db.run(VehicleRepository.deleteVin(ns, vin)))
    }

  def getPackages(ns: Namespace, vin: Vehicle.Vin): Route =
    completeOrRecoverWith(db.run(VehicleRepository.packagesOnVin(ns, vin))) {
      Errors.onMissingVehicle
    }

  def installPackage(ns: Namespace, vin: Vehicle.Vin, pkgId: PackageId): Route =
    completeOrRecoverWith(db.run(VehicleRepository.installPackage(ns, vin, pkgId))) {
      Errors.onMissingVehicle orElse Errors.onMissingPackage
    }

  def uninstallPackage(ns: Namespace, vin: Vehicle.Vin, pkgId: PackageId): Route =
    completeOrRecoverWith(db.run(VehicleRepository.uninstallPackage(ns, vin, pkgId))) {
      Errors.onMissingVehicle orElse Errors.onMissingPackage
    }

  def updateInstalledSoftware(ns: Namespace, vin: Vehicle.Vin): Route =
    entity(as[InstalledSoftware]) { installedSoftware =>
      val dbIO = DBIO.seq(
        VehicleRepository.updateInstalledPackages(ns, vin, installedSoftware.packages),
        VehicleRepository.updateInstalledFirmware(ns, vin, installedSoftware.firmware)
      )

      complete { db.run(dbIO).map(_ => StatusCodes.NoContent) }
    }

  /**
   * API route for package -> vehicle associations.
   *
   * @return      Route object containing routes for listing packages on a vehicle, and creating and deleting
   *              vehicle -> package associations
   * @throws      Errors.MissingPackageException if package doesn't exist
   * @throws      Errors.MissingVehicle if vehicle doesn't exist
   */
  def packageApi(vin: Vehicle.Vin): Route = {
    (pathPrefix("package") & extractNamespace) { ns =>
      (get & pathEnd) {
        getPackages(ns, vin)
      } ~
      refinedPackageId { pkgId =>
        (put & pathEnd) {
          installPackage(ns, vin, pkgId)
        } ~
        (delete & pathEnd) {
          uninstallPackage(ns, vin, pkgId)
        }
      }
    } ~
    (path("packages") & put & handleExceptions(installedPackagesHandler) & extractNamespace ) { ns =>
      updateInstalledSoftware(ns, vin)
    }
  }

  def getComponents(ns: Namespace, vin: Vehicle.Vin): Route =
    completeOrRecoverWith(db.run(VehicleRepository.componentsOnVin(ns, vin))) {
        Errors.onMissingVehicle
      }

  def installComponent(ns: Namespace, vin: Vehicle.Vin, part: Component.PartNumber): Route =
    complete(db.run(VehicleRepository.installComponent(ns, vin, part)))

  def uninstallComponent(ns: Namespace, vin: Vehicle.Vin, part: Component.PartNumber): Route =
    complete(db.run(VehicleRepository.uninstallComponent(ns, vin, part)))

  /**
   * API route for component -> vehicle associations.
   *
   * @return      Route object containing routes for listing components on a vehicle, and creating and deleting
   *              vehicle -> component associations
   * @throws      Errors.MissingComponent if component doesn't exist
   * @throws      Errors.MissingVehicle if vehicle doesn't exist
   */
  def componentApi(vin: Vehicle.Vin): Route =
    (pathPrefix("component") & extractNamespace) { ns =>
      (get & pathEnd) {
        getComponents(ns, vin)
      } ~
      (refinedPartNumber & handleExceptions(installedComponentsHandler)) { part =>
        (put & pathEnd) {
          installComponent(ns, vin, part)
        } ~
        (delete & pathEnd) {
          uninstallComponent(ns, vin, part)
        }
      }
    }

  def vehicleApi: Route =
    (pathPrefix("vehicles") & extractNamespace) { ns =>
      (get & pathEnd) {
        searchVehicles(ns)
      } ~
      extractVin { vin =>
        (get & pathEnd) {
          getVehicle(ns, vin)
        } ~
        (put & pathEnd) {
          addVehicle(ns, vin)
        } ~
        (delete & pathEnd) {
          deleteVehicle(ns, vin)
        } ~
        packageApi(vin) ~
        componentApi(vin)
      }
    }

  def getFirmware(ns: Namespace, vin: Vehicle.Vin): Route =
    completeOrRecoverWith(db.run(VehicleRepository.firmwareOnVin(ns, vin))) {
      Errors.onMissingVehicle
    }

  /**
   * Base API route for vehicles.
   *
   * @return      Route object containing routes for creating, deleting, and listing vehicles
   * @throws      Errors.MissingVehicle if vehicle doesn't exist
   */
  def route: Route = {
    vehicleApi ~
    (pathPrefix("firmware") & get & extractNamespace & extractVin) { (ns, vin) =>
        getFirmware(ns, vin)
    }
  }

}
