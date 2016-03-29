/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.PathMatchers.Slash
import akka.http.scaladsl.server.{Directive1, Directives, ExceptionHandler, PathMatchers}
import Directives._
import akka.parboiled2.util.Base64
import akka.stream.ActorMaterializer
import akka.stream.io.SynchronousFileSink
import akka.util.ByteString
import cats.data.Xor
import eu.timepit.refined._
import eu.timepit.refined.string._
import io.circe.generic.auto._
import java.io.File
import org.genivi.sota.core.data._
import org.genivi.sota.core.db._
import org.genivi.sota.core.db.{UpdateSpecs, Packages, Vehicles, InstallHistories}
import org.genivi.sota.core.rvi.ServerServices
import org.genivi.sota.core.transfer.UpdateNotifier
import org.genivi.sota.data.Vehicle
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.rest.Validation._
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import org.joda.time.DateTime
import scala.concurrent.Future
import scala.util.Failure
import slick.dbio.DBIO
import slick.driver.MySQLDriver.api.Database


object ErrorCodes {
  val ExternalResolverError = ErrorCode( "external_resolver_error" )

  val MissingVehicle = new ErrorCode("missing_vehicle")
}

class VehiclesResource(db: Database, client: ConnectivityClient)
                      (implicit system: ActorSystem, mat: ActorMaterializer) {

  import system.dispatcher
  import CirceMarshallingSupport._

  import scala.concurrent.{ExecutionContext, Future}

  case object MissingVehicle extends Throwable

  def exists
    (vehicle: Vehicle)
    (implicit ec: ExecutionContext): Future[Vehicle] =
    db.run(Vehicles.exists(vehicle.vin))
      .flatMap(_
        .fold[Future[Vehicle]]
          (Future.failed(MissingVehicle))(Future.successful(_)))

  def deleteVin (vehicle: Vehicle)
  (implicit ec: ExecutionContext): Future[Unit] =
    for {
      _ <- exists(vehicle)
      _ <- db.run(UpdateSpecs.deleteRequiredPackageByVin(vehicle))
      _ <- db.run(UpdateSpecs.deleteUpdateSpecByVin(vehicle))
      _ <- db.run(Vehicles.deleteById(vehicle))
    } yield ()


  val extractVin : Directive1[Vehicle.Vin] = refined[Vehicle.ValidVin](Slash ~ Segment)

  def ttl() : DateTime = {
    import com.github.nscala_time.time.Implicits._
    DateTime.now + 5.minutes
  }

  val route = pathPrefix("vehicles") {
    extractVin { vin =>
      pathEnd {
        get {
          completeOrRecoverWith(exists(Vehicle(vin))) {
            case MissingVehicle =>
              complete(StatusCodes.NotFound ->
                ErrorRepresentation(ErrorCodes.MissingVehicle, "Vehicle doesn't exist"))
          }
        } ~
        put {
          complete(db.run(Vehicles.create(Vehicle(vin))).map(_ => NoContent))
        } ~
        delete {
          completeOrRecoverWith(deleteVin(Vehicle(vin))) {
            case MissingVehicle =>
              complete(StatusCodes.NotFound ->
                ErrorRepresentation(ErrorCodes.MissingVehicle, "Vehicle doesn't exist"))
          }
        }
      } ~
      // TODO: Check that vin exists
      (path("queued") & get) {
        complete(db.run(UpdateSpecs.getPackagesQueuedForVin(vin)))
      } ~
      (path("history") & get) {
        complete(db.run(InstallHistories.list(vin)))
      } ~
      (path("sync") & put) {
        // TODO: Config RVI destination path (or ClientServices.getpackages)
        client.sendMessage(s"genivi.org/vin/${vin}/sota/getpackages", io.circe.Json.Empty, ttl())
        // TODO: Confirm getpackages in progress to vehicle?
        complete(NoContent)
      }
    } ~
    pathEnd {
      get {
        parameters('regex.?) { (regex) =>
          val query = regex match {
            case Some(r) => Vehicles.searchByRegex(r)
            case _ => Vehicles.list()
          }
          complete(db.run(query))
        }
      }
    }
  }
}

class UpdateRequestsResource(db: Database, resolver: ExternalResolverClient, updateService: UpdateService)
                            (implicit system: ActorSystem, mat: ActorMaterializer) {
  import system.dispatcher
  import eu.timepit.refined.string.uuidValidate
  import org.genivi.sota.core.db.UpdateSpecs
  import UpdateSpec._
  import CirceMarshallingSupport._

  implicit val _db = db
  val route = pathPrefix("updates") {
    (get & refined[Uuid](Slash ~ Segment ~ PathEnd)) { uuid =>
      complete(db.run(UpdateSpecs.listUpdatesById(uuid)))
    }
  } ~
  path("updates") {
    get {
      complete(updateService.all(db, system.dispatcher))
    } ~
    post {
      entity(as[UpdateRequest]) { req =>
        complete(
          updateService.queueUpdate(
            req,
            pkg => resolver.resolve(pkg.id).map {
              m => m.map { case (v, p) => (v.vin, p) }
            }
          )
        )
      }
    }
  }
}


class WebService(notifier: UpdateNotifier, resolver: ExternalResolverClient, db : Database)
                (implicit system: ActorSystem, mat: ActorMaterializer,
                 connectivity: Connectivity) extends Directives {
  implicit val log = Logging(system, "webservice")

  import io.circe.Json
  import Json.{obj, string}

  val exceptionHandler = ExceptionHandler {
    case e: Throwable =>
      extractUri { uri =>
        log.error(s"Request to $uri errored: $e")
        val entity = obj("error" -> string(e.getMessage()))
        complete(HttpResponse(InternalServerError, entity = entity.toString()))
      }
  }
  val vehicles = new VehiclesResource(db, connectivity.client)
  val packages = new PackagesResource(resolver, db)
  val updateRequests = new UpdateRequestsResource(db, resolver, new UpdateService(notifier))

  val route = pathPrefix("api" / "v1") {
    handleExceptions(exceptionHandler) {
       vehicles.route ~ packages.route ~ updateRequests.route
    }
  }

}
