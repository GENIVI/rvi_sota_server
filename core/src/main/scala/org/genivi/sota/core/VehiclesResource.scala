/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import io.circe.generic.auto._
import io.circe.syntax._
import org.genivi.sota.common.IDeviceRegistry
import org.genivi.sota.core.common.NamespaceDirective._
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.{InstallHistories, UpdateSpecs}
import org.genivi.sota.core.resolver.{ConnectivityClient, ExternalResolverClient}
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{Device, Vehicle}
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.rest.ErrorRepresentation
import org.genivi.sota.rest.Validation._
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import scala.languageFeature.implicitConversions
import scala.languageFeature.postfixOps
import slick.driver.MySQLDriver.api.Database
import org.genivi.sota.core.db.Vehicles.VehicleTable
import org.genivi.sota.core.db.UpdateSpecs.{RequiredPackageTable, UpdateSpecTable}


class VehiclesResource(db: Database,
                       client: ConnectivityClient,
                       resolverClient: ExternalResolverClient,
                       deviceRegistry: IDeviceRegistry)
                      (implicit system: ActorSystem, mat: ActorMaterializer) {

  import CirceMarshallingSupport._
  import Directives._
  import WebService._
  import system.dispatcher

  implicit val _db = db

  case object MissingVehicle extends Throwable

  /**
    * An ota client GET a Seq of [[Vehicle]] from regex/status search.
    */
  def search(ns: Namespace): Route = {
    parameters(('status.?(false), 'regex.?)) { (includeStatus: Boolean, regex: Option[String]) =>
      if (includeStatus) {
        val devicesAndVinsWithStatus = for {
          devices <- regex match {
            case Some(re) => deviceRegistry.searchDevice(refineV[Regex](re).right.get) // TODO error handling
            case None => deviceRegistry.searchDevice(Refined.unsafeApply(".*")) // TODO .list / pagination
          }
          vinsWithStatus <- db.run(VehicleSearch.vinsWithStatus(ns))
        } yield (devices, vinsWithStatus)

        val devicesWithStatus = devicesAndVinsWithStatus.map { case (devices, vinsWithStatus) => for {
          d <- devices
          status = vinsWithStatus.filter(_.vin.get == d.deviceId)
                     .headOption
                     .map(_.status)
                     .getOrElse(VehicleStatus.NotSeen)
        } yield (VehicleUpdateStatus(refineV[Vehicle.ValidVin](d.deviceId.get.underlying).right.get, status, None)) }

        onComplete(devicesWithStatus) {
          case Success(ds) => complete(ds.asJson)
          case Failure(ex) =>
            complete((StatusCodes.InternalServerError, "error: cannot lookup update status for devices"))
        }
      } else {
        // redirect to device registry
        val devices = regex match {
          case Some(re) => deviceRegistry.searchDevice(refineV[Regex](re).right.get) // TODO error handling
          case None => deviceRegistry.searchDevice(Refined.unsafeApply(".*")) // TODO .list / pagination
        }

        onComplete(devices) {
          case Success(ds) => complete(ds.asJson)
          case Failure(ex) => complete((StatusCodes.InternalServerError, "error: cannot lookup devices"))
        }
      }
    }
  }

  val route =
    (pathPrefix("vehicles") & extractNamespace) { ns =>
      (pathEnd & get) {
        search(ns)
      }
    }
}
