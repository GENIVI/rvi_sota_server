/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import org.genivi.sota.core.resolver.ConnectivityClient
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import Directives._
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.string._
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import akka.http.scaladsl.marshalling.Marshaller._
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.{InstallHistories, UpdateSpecs, Vehicles}
import org.genivi.sota.data.Vehicle
import org.genivi.sota.rest.Validation._
import org.genivi.sota.rest.ErrorRepresentation
import org.joda.time.DateTime
import slick.driver.MySQLDriver.api.Database

import scala.concurrent.{ExecutionContext, Future}
import io.circe.syntax._
import org.genivi.sota.core.resolver.ExternalResolverClient
import scala.languageFeature.postfixOps
import scala.languageFeature.implicitConversions

class VehiclesResource(db: Database, client: ConnectivityClient, resolverClient: ExternalResolverClient)
                      (implicit system: ActorSystem, mat: ActorMaterializer) {

  import system.dispatcher
  import CirceMarshallingSupport._
  import WebService._

  implicit val _db = db

  case object MissingVehicle extends Throwable

  def exists(vehicle: Vehicle)
    (implicit ec: ExecutionContext): Future[Vehicle] =
    db.run(Vehicles.exists(vehicle.vin))
      .flatMap(_
        .fold[Future[Vehicle]]
          (Future.failed(MissingVehicle))(Future.successful))

  def deleteVin (vehicle: Vehicle)
  (implicit ec: ExecutionContext): Future[Unit] =
    for {
      _ <- exists(vehicle)
      _ <- db.run(UpdateSpecs.deleteRequiredPackageByVin(vehicle))
      _ <- db.run(UpdateSpecs.deleteUpdateSpecByVin(vehicle))
      _ <- db.run(Vehicles.deleteById(vehicle))
    } yield ()


  def ttl() : DateTime = {
    import com.github.nscala_time.time.Implicits._
    DateTime.now + 5.minutes
  }


  def fetchVehicle(vin: Vehicle.Vin)  = {
    completeOrRecoverWith(exists(Vehicle(vin))) {
      case MissingVehicle =>
        complete(StatusCodes.NotFound ->
          ErrorRepresentation(ErrorCodes.MissingVehicle, "Vehicle doesn't exist"))
    }
  }

  def updateVehicle(vin: Vehicle.Vin) = {
    complete(db.run(Vehicles.create(Vehicle(vin))).map(_ => NoContent))
  }

  def deleteVehicle(vin: Vehicle.Vin) = {
    completeOrRecoverWith(deleteVin(Vehicle(vin))) {
      case MissingVehicle =>
        complete(StatusCodes.NotFound ->
          ErrorRepresentation(ErrorCodes.MissingVehicle, "Vehicle doesn't exist"))
    }
  }

  def queuedPackages(vin: Vehicle.Vin) = {
    complete(db.run(UpdateSpecs.getPackagesQueuedForVin(vin)))
  }

  def history(vin: Vehicle.Vin) = {
    complete(db.run(InstallHistories.list(vin)))
  }

  def sync(vin: Vehicle.Vin) = {
    // TODO: Config RVI destination path (or ClientServices.getpackages)
    client.sendMessage(s"genivi.org/vin/${vin}/sota/getpackages", io.circe.Json.Empty, ttl())
    // TODO: Confirm getpackages in progress to vehicle?
    complete(NoContent)
  }

  def search = {
    parameters(('status.?(false), 'regex.?)) { (includeStatus: Boolean, regex: Option[String]) =>
      val resultIO = VehicleSearch.search(regex, includeStatus)
      complete(db.run(resultIO))
    }
  }

  val route = {
    pathPrefix("vehicles") {
      extractVin { vin =>
        pathEnd {
          get {
            fetchVehicle(vin)
          } ~
          put {
            updateVehicle(vin)
          } ~
          delete {
            deleteVehicle(vin)
          }
        } ~
        (path("queued") & get) {
          queuedPackages(vin)
        } ~
        (path("history") & get) {
          history(vin)
        } ~
        (path("sync") & put) {
          sync(vin)
        }
      } ~
      (pathEnd & get) {
        search
      }
    }
  }
}
