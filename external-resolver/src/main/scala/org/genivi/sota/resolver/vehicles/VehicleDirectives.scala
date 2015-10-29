/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.vehicles

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server._
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import eu.timepit.refined.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.common.RefinementDirectives.{refinedPackageId, refinedPartNumber}
import org.genivi.sota.resolver.components.{Component, ComponentRepository}
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.rest.Validation._
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import slick.dbio.{DBIOAction, DBIO}
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend.Database


class VehicleDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {
  import Directives._

  def installedPackagesHandler = ExceptionHandler(Errors.onMissingPackage orElse Errors.onMissingVehicle)

  def route: Route = {

    val extractVin : Directive1[Vehicle.Vin] = refined[Vehicle.ValidVin](Slash ~ Segment)

    pathPrefix("vehicles") {
      get {
        pathEnd {
          parameters('regex.as[Refined[String, Regex]].?, 'packageName.as[Package.Name].?,
            'packageVersion.as[Package.Version].?, 'component.as[Component.PartNumber].?)
          { case (re, pn, pv, cp) =>
              complete(db.run(VehicleRepository.search(re, pn , pv, cp)))
          }
        }
      } ~
      extractVin { vin =>
        put {
          pathEnd {
            complete(db.run(VehicleRepository.add(Vehicle(vin))).map(_ => NoContent))
          }
        } ~
        handleExceptions(installedPackagesHandler) {
          delete {
            pathEnd {
              complete(db.run(VehicleRepository.deleteVin(vin)))
            }
          }
        } ~
        packageRoute(vin) ~
        componentRoute(vin)
      }
    }
  }

  def packageRoute(vin: Vehicle.Vin): Route = {
    pathPrefix("package") {
      (pathEnd & get) {
        completeOrRecoverWith(db.run(VehicleRepository.packagesOnVin(vin))) {
          Errors.onMissingVehicle
        }
      } ~
      refinedPackageId { pkgId =>
        put(
          completeOrRecoverWith(db.run(VehicleRepository.installPackage(vin, pkgId))) {
            Errors.onMissingVehicle orElse Errors.onMissingPackage
          }
        ) ~
        delete (
          completeOrRecoverWith(db.run(VehicleRepository.uninstallPackage(vin, pkgId))) {
            Errors.onMissingVehicle orElse Errors.onMissingPackage
          }
        )
      }
    } ~
    path("packages") {
      handleExceptions(installedPackagesHandler) {
        (put & entity(as[Set[Package.Id]]) ) { packageIds =>
          onSuccess( VehicleFunctions.updateInstalledPackages(vin, packageIds ) ) {
            complete( StatusCodes.NoContent )
          }
        }
      }
    }
  }

  def installedComponentsHandler =
    ExceptionHandler(Errors.onMissingVehicle orElse Errors.onMissingComponent)

  def componentRoute(vin: Vehicle.Vin): Route = {
    pathPrefix("component") {
      (pathEnd & get) {
        completeOrRecoverWith(db.run(VehicleRepository.componentsOnVin(vin))) {
            Errors.onMissingVehicle
          }
      } ~
      refinedPartNumber
      { part =>
        handleExceptions(installedComponentsHandler) {
          put {
            complete(db.run(VehicleRepository.installComponent(vin, part)))
          } ~
          delete {
            complete(db.run(VehicleRepository.uninstallComponent(vin, part)))
          }
        }
      }
    }
  }

}
