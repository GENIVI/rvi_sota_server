/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
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
import org.genivi.sota.core.db.{OperationResults, UpdateSpecs, Vehicles}
import org.genivi.sota.data.Namespace._
import org.genivi.sota.core.data.client.ResponseConversions
import org.genivi.sota.core.resolver.{Connectivity, DefaultConnectivity, ExternalResolverClient}
import org.genivi.sota.core.storage.PackageStorage
import org.joda.time.DateTime
import org.genivi.sota.core.data.{UpdateRequest, UpdateSpec}
import org.genivi.sota.core.data.client.PendingUpdateRequest
import org.genivi.sota.core.data.UpdateStatus

import scala.language.implicitConversions

class VehicleUpdatesResource(db : Database, resolverClient: ExternalResolverClient,
                             namespaceExtractor: Directive1[Namespace])
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

  def logVehicleSeen(vin: Vehicle.Vin): Directive0 = {
    extractRequestContext flatMap { _ =>
      onComplete(db.run(Vehicles.updateLastSeen(vin)))
    } flatMap (_ => pass)
  }

  /**
    * An ota client PUT a list of packages to record they're installed on a vehicle, overwriting any previous such list.
    */
  def updateInstalledPackages(vin: Vehicle.Vin): Route = {
    entity(as[List[PackageId]]) { ids =>
      val f = VehicleUpdates
        .update(vin, ids, resolverClient)
        .map(_ => NoContent)

      complete(f)
    }
  }

  /**
    * An ota client GET which packages await installation for the given vehicle,
    * in the form a Seq of [[PendingUpdateRequest]]
    * whose order was specified via [[setInstallOrder]].
    * To actually download each binary file, [[downloadPackage]] is used.
    *
    * @see [[data.UpdateStatus]] (two of interest: InFlight and Pending)
    */
  def pendingPackages(vin: Vehicle.Vin): Route = {
    import org.genivi.sota.core.data.client.PendingUpdateRequest._
    import ResponseConversions._

    val vehiclePackages =
      VehicleUpdates
        .findPendingPackageIdsFor(vin)
        .map(_.toResponse)

    complete(db.run(vehiclePackages))
  }

  /**
    * An ota client GET the binary file for the package that the given [[UpdateRequest]] and [[Vehicle]] refers to.
    */
  def downloadPackage(vin: Vehicle.Vin, uuid: Refined[String, Uuid]): Route = {
    withRangeSupport {
      complete(packageDownloadProcess.buildClientDownloadResponse(vin, uuid))
    }
  }

  /**
    * An ota client POST for the given [[UpdateRequest]] an [[InstallReport]]
    * (describing the outcome after installing the package in question).
    */
  def reportInstall(uuid: Refined[String, Uuid]): Route = {
    entity(as[InstallReport]) { report =>
      val responseF =
        VehicleUpdates
          .buildReportInstallResponse(report.vin, report.update_report)
      complete(responseF)
    }
  }

  /**
    * A web client fetches the results of updates to a given [[Vehicle]].
    */
  def results(vin: Vehicle.Vin): Route = {
    complete(db.run(OperationResults.byVin(vin)))
  }

  /**
    * An ota client POST a [[PackageId]] to schedule installation on a vehicle.
    * Internally an [[UpdateRequest]] and an [[UpdateSpec]] are persisted for that [[PackageId]].
    * Resolver is not contacted.
    */
  def queueVehicleUpdate(ns: Namespace, vin: Vehicle.Vin): Route = {
    entity(as[PackageId]) { packageId =>
      val result = updateService.queueVehicleUpdate(ns, vin, packageId)
      complete(result)
    }
  }

  def sync(vin: Vehicle.Vin): Route = {
    val ttl = DateTime.now.plusMinutes(5)
    // TODO: Config RVI destination path (or ClientServices.getpackages)
    // TODO: pass namespace
    connectivity.client.sendMessage(s"genivi.org/vin/${vin.get}/sota/getpackages", io.circe.Json.Null, ttl)
    // TODO: Confirm getpackages in progress to vehicle?
    complete(NoContent)
  }

  /**
    * The web app PUT the order in which the given [[UpdateRequest]]s are to be installed on a vehicle.
    */
  def setInstallOrder(vin: Vehicle.Vin): Route = {
    entity(as[Map[Int, UUID]]) { uuids =>
      val sorted: List[UUID] = uuids.toList.sortBy(_._1).map(_._2)
      val resp = VehicleUpdates.buildSetInstallOrderResponse(vin, sorted)
      complete(resp)
    }
  }

  /**
    * The web app PUT the status of the given ([[UpdateSpec]], VIN) to [[UpdateStatus.Canceled]]
    */
  def cancelUpdate(vin: Vehicle.Vin, uuid: Refined[String, Uuid]): Route = {
    val response = db.run(UpdateSpecs.cancelUpdate(vin, uuid)).map {
      i: Int => i match {
          case 0 => HttpResponse(StatusCodes.BadRequest)
          case _ => HttpResponse(StatusCodes.NoContent)
        }
    }
    complete(response)
  }

  val route = {
    (pathPrefix("api" / "v1" / "vehicle_updates") & extractVin) { vin =>
      (put & path("installed")) { updateInstalledPackages(vin) } ~
      (put & path("order")) { setInstallOrder(vin) } ~
      (put & extractUuid & path("cancelupdate") ) { uuid => cancelUpdate(vin, uuid) } ~
      (post & namespaceExtractor & pathEnd) { ns => queueVehicleUpdate(ns, vin) } ~
      (get & pathEnd) { logVehicleSeen(vin) { pendingPackages(vin) } } ~
      (get & path("queued")) { pendingPackages(vin) } ~
      (get & extractUuid & path("download")) { uuid => downloadPackage(vin, uuid) } ~
      (get & path("operationresults")) { results(vin) } ~
      (post & extractUuid) { reportInstall } ~
      (post & path("sync")) { sync(vin) }
    }
  }
}
