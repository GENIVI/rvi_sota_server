/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.stream.ActorMaterializer
import eu.timepit.refined.string.Uuid
import org.genivi.sota.core.rvi.InstallReport
import org.genivi.sota.core.transfer.{InstalledPackagesUpdate, PackageDownloadProcess}
import org.genivi.sota.data.PackageId
import slick.driver.MySQLDriver.api.Database
import io.circe.generic.auto._
import org.genivi.sota.rest.Validation.refined
import org.genivi.sota.marshalling.CirceMarshallingSupport._

class VehicleService(db : Database, resolverClient: ExternalResolverClient)
                    (implicit system: ActorSystem, mat: ActorMaterializer,
                     connectivity: Connectivity) extends Directives {
  implicit val log = Logging(system, "vehicleservice")

  import io.circe.Json
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

  val extractUuid = refined[Uuid](Slash ~ Segment)

  implicit val ec = system.dispatcher
  implicit val _db = db

  val route = pathPrefix("api" / "v1" / "vehicles") {
    handleExceptions(exceptionHandler) {
      vehicles.route ~
        WebService.extractVin { vin =>
          pathPrefix("updates") {
            (pathEnd & post) {
              entity(as[List[PackageId]]) { ids =>
                val f = InstalledPackagesUpdate
                  .update(vin, ids, resolverClient)
                  .map(_ => NoContent)

                complete(f)
              }
            } ~
              (pathEnd & get) {
                val responseF = new PackageDownloadProcess(db).buildClientPendingIdsResponse(vin)
                complete(responseF)
              } ~
              (get & withRangeSupport & extractUuid & path("download")) { uuid =>
                val responseF = new PackageDownloadProcess(db).buildClientDownloadResponse(uuid)
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
