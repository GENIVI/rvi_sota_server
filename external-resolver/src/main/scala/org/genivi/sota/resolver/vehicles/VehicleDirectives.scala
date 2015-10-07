/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.vehicles

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import eu.timepit.refined.Refined
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPackageId
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.rest.Validation._
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend.Database


class VehicleDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {
  import Directives._

  def installedPackagesHandler = ExceptionHandler( Errors.onMissingPackage orElse Errors.onMissingVehicle)

  def route(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Route = {
    val extractVin : Directive1[Vehicle.Vin] = refined[Vehicle.ValidVin](Slash ~ Segment)
    pathPrefix("vehicles") {
      get {
        pathEnd {
          parameter('package.as[Package.NameVersion].?) {
            case Some(nameVersion) =>
              val packageName   : Package.Name    = Refined(nameVersion.get.split("-").head)
              val packageVersion: Package.Version = Refined(nameVersion.get.split("-").tail.head)
              completeOrRecoverWith(VehicleFunctions.vinsThatHavePackage(Package.Id(packageName, packageVersion))) {
                Errors.onMissingPackage
              }
            case None =>
              complete(db.run(VehicleRepository.list))
          }
        }
      } ~
      extractVin { vin =>
        put {
          pathEnd {
            complete(db.run(VehicleRepository.add(Vehicle(vin))).map(_ => NoContent))
          }
        } ~
        handleExceptions(ExceptionHandler( installedPackagesHandler ) ) {
          pathPrefix("package") {
            (pathEnd & get)( complete(VehicleFunctions.packagesOnVin(vin)) ) ~
              (handleExceptions( installedPackagesHandler ) & refinedPackageId) { pkgId =>
              put(
                complete(VehicleFunctions.installPackage(vin, pkgId))
              ) ~
              delete (
                complete(VehicleFunctions.uninstallPackage(vin, pkgId))
              )
            }
          } ~
          path("packages") {
            (put & entity(as[Set[Package.Id]]) ) { packageIds =>
              onSuccess( VehicleFunctions.updateInstalledPackages(vin, packageIds ) ) {
                complete( StatusCodes.NoContent )
              }
            }
          }
        }
      }
    }
  }
}
