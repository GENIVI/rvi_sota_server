/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import org.genivi.sota.core.rvi.InstallReport
import org.genivi.sota.core.transfer.{DefaultUpdateNotifier, PackageDownloadProcess, VehicleUpdates}
import org.genivi.sota.data.{PackageId, Vehicle}
import slick.driver.MySQLDriver.api.Database
import io.circe.generic.auto._
import org.genivi.sota.core.db.Vehicles
import org.genivi.sota.core.common.NamespaceDirective._
import org.genivi.sota.data.Namespace._
import org.genivi.sota.rest.Validation.refined
import org.genivi.sota.core.data.client.ResponseConversions
import org.genivi.sota.core.resolver.{Connectivity, DefaultConnectivity, ExternalResolverClient}
import org.genivi.sota.core.storage.PackageStorage
import org.joda.time.DateTime

import scala.language.implicitConversions


class VehicleUpdatesResource(db : Database, resolverClient: ExternalResolverClient)
                            (implicit system: ActorSystem, mat: ActorMaterializer,
                             connectivity: Connectivity = DefaultConnectivity) extends Directives {

  import WebService._
  import org.genivi.sota.marshalling.CirceMarshallingSupport._

  implicit val ec = system.dispatcher
  implicit val _db = db
  implicit val _config = system.settings.config

  lazy val packageRetrievalOp = (new PackageStorage).retrieveResponse _

  lazy val packageDownloadProcess = new PackageDownloadProcess(db, packageRetrievalOp)

  protected lazy val updateService = new UpdateService(DefaultUpdateNotifier)

  def logVehicleSeen(vehicle: Vehicle): Directive0 = {
    extractRequestContext flatMap { _ =>
      onComplete(db.run(Vehicles.updateLastSeen(vehicle)))
    } flatMap (_ => pass)
  }

  def updateInstalledPackages(vin: Vehicle.Vin): Route = {
    entity(as[List[PackageId]]) { ids =>
      val f = VehicleUpdates
        .update(vin, ids, resolverClient)
        .map(_ => NoContent)

      complete(f)
    }
  }

  def pendingPackages(ns: Namespace, vin: Vehicle.Vin): Route = {
    import org.genivi.sota.core.data.client.PendingUpdateRequest._
    import ResponseConversions._

    logVehicleSeen(Vehicle(ns, vin)) {
      val vehiclePackages =
        VehicleUpdates
          .findPendingPackageIdsFor(ns, vin)
          .map(_.toResponse)

      complete(db.run(vehiclePackages))
    }
  }

  def downloadPackage(uuid: Refined[String, Uuid]): Route = {
    withRangeSupport {
      val responseF = packageDownloadProcess.buildClientDownloadResponse(uuid)
      complete(responseF)
    }
  }

  def reportInstall(uuid: Refined[String, Uuid]): Route = {
    entity(as[InstallReport]) { report =>
      val responseF =
        VehicleUpdates
          .buildReportInstallResponse(report.vin, report.update_report)
      complete(responseF)
    }
  }

  def queueVehicleUpdate(ns: Namespace, vin: Vehicle.Vin): Route = {
    entity(as[PackageId]) { packageId =>
      val result = updateService.queueVehicleUpdate(ns, vin, packageId)
      complete(result)
    }
  }

  def sync(ns: Namespace, vin: Vehicle.Vin): Route = {
    val ttl = DateTime.now.plusMinutes(5)
    // TODO: Config RVI destination path (or ClientServices.getpackages)
    // TODO: pass namespace
    connectivity.client.sendMessage(s"genivi.org/vin/${vin.get}/sota/getpackages", io.circe.Json.Null, ttl)
    // TODO: Confirm getpackages in progress to vehicle?
    complete(NoContent)
  }

  def setInstallOrder(vin: Vehicle.Vin): Route = {
    entity(as[Map[Int, UUID]]) { uuids =>
      val sorted = uuids.toList.sortBy(_._1).map(_._2)
      val resp = VehicleUpdates.buildSetInstallOrderResponse(vin, sorted)
      complete(resp)
    }
  }

  val route = {
    (pathPrefix("api" / "v1" / "vehicle_updates") & extractVin) { vin =>
      (put & path("installed")) { updateInstalledPackages(vin) } ~
      (put & path("order")) { setInstallOrder(vin) } ~
      (post & extractNamespace & pathEnd) { ns => queueVehicleUpdate(ns, vin) } ~
      (get & extractNamespace & pathEnd) { ns => pendingPackages(ns, vin) } ~
      (get & extractUuid & path("download")) { downloadPackage } ~
      (post & extractUuid) { reportInstall } ~
      (post & extractNamespace & path("sync")) { ns => sync(ns, vin) }
    }
  }
}
