/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{Directive0, Directives, ExceptionHandler}
import akka.stream.ActorMaterializer
import eu.timepit.refined.string.Uuid
import org.genivi.sota.core.rvi.InstallReport
import org.genivi.sota.core.transfer.{InstalledPackagesUpdate, PackageDownloadProcess}
import org.genivi.sota.data.{PackageId, Vehicle}
import slick.driver.MySQLDriver.api.Database
import io.circe.generic.auto._
import org.genivi.sota.core.data.VehicleUpdateStatus
import org.genivi.sota.core.db.Vehicles
import org.genivi.sota.rest.Validation.refined
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import io.circe.Json


class VehicleService(db : Database, resolverClient: ExternalResolverClient)
                    (implicit system: ActorSystem, mat: ActorMaterializer,
                     connectivity: Connectivity) extends Directives {
  implicit val log = Logging(system, "vehicleservice")

  import Json.{obj, string}

  val exceptionHandler = ExceptionHandler {
    case e: Throwable =>
      extractUri { uri =>
        log.error(s"Request to $uri failed: $e")
        val entity = obj("error" -> string(e.getMessage))
        complete(HttpResponse(InternalServerError, entity = entity.toString()))
      }
  }

  val vehicles = new VehiclesResource(db, connectivity.client, resolverClient)

  val packageDownloadProcess = new PackageDownloadProcess(db)

  val extractUuid = refined[Uuid](Slash ~ Segment)

  implicit val ec = system.dispatcher
  implicit val _db = db

  def logVehicleSeen(vin: Vehicle.Vin): Directive0 = {
    extractRequestContext flatMap { _ =>
      onComplete(db.run(Vehicles.updateLastSeen(vin)))
    } flatMap (_ => pass)
  }

  val route = pathPrefix("api" / "v1") {
    handleExceptions(exceptionHandler) {
      vehicles.route ~
      pathPrefix("vehicles") {
        WebService.extractVin { vin =>
          path("status") {
            val io = for {
              specs <- VehicleUpdateStatus.findPackagesFor(vin)
              vehicle <- Vehicles.findBy(vin)
            } yield VehicleUpdateStatus.current(vehicle, specs)

            complete(db.run(io))
          } ~
          pathPrefix("updates") {
            (pathEnd & post) {
              entity(as[List[PackageId]]) { ids =>
                val f = InstalledPackagesUpdate
                  .update(vin, ids, resolverClient)
                  .map(_ => NoContent)

                complete(f)
              }
            } ~
              (get & logVehicleSeen(vin) & pathEnd) {
                val vehiclePackages = VehicleUpdateStatus.findPendingPackageIdsFor(vin)
                complete(db.run(vehiclePackages))
              } ~
              (get & withRangeSupport & extractUuid & path("download")) { uuid =>
                val responseF = packageDownloadProcess.buildClientDownloadResponse(uuid)
                complete(responseF)
              } ~
              (post & extractUuid) { uuid =>
                entity(as[InstallReport]) { report =>
                  val responseF =
                    InstalledPackagesUpdate
                      .buildReportInstallResponse(report.vin, report.update_report)

                  complete(responseF)
              }
            }
          }
        }
        }
    }
  }
}
