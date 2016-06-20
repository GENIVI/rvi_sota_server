/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import slick.dbio.DBIO
import io.circe.generic.auto._
import java.util.UUID

import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.data.client.ResponseConversions
import org.genivi.sota.core.db.{BlockedInstalls, OperationResults, UpdateSpecs}
import org.genivi.sota.core.resolver.{Connectivity, DefaultConnectivity, ExternalResolverClient}
import org.genivi.sota.core.rvi.InstallReport
import org.genivi.sota.core.storage.PackageStorage
import java.time.Instant
import java.time.temporal.ChronoUnit

import org.genivi.sota.core.transfer.DeviceUpdates
import org.genivi.sota.core.data.{UpdateRequest, UpdateSpec}
import org.genivi.sota.core.data.client.PendingUpdateRequest
import org.genivi.sota.core.data.UpdateStatus
import org.genivi.sota.core.transfer.{DefaultUpdateNotifier, PackageDownloadProcess}
import org.genivi.sota.data.{Device, PackageId}
import org.genivi.sota.data.Namespace.Namespace

import scala.language.implicitConversions
import slick.driver.MySQLDriver.api.Database
import cats.Show

class DeviceUpdatesResource(db: Database,
                            resolverClient: ExternalResolverClient,
                            deviceRegistry: DeviceRegistry,
                            namespaceExtractor: Directive1[Namespace])
                           (implicit system: ActorSystem, mat: ActorMaterializer,
                            connectivity: Connectivity = DefaultConnectivity) {

  import Directives._
  import WebService._
  import org.genivi.sota.marshalling.CirceMarshallingSupport._
  import Device._

  implicit val ec = system.dispatcher
  implicit val _db = db
  implicit val _config = system.settings.config

  lazy val packageRetrievalOp = (new PackageStorage).retrieveResponse _

  lazy val packageDownloadProcess = new PackageDownloadProcess(db, packageRetrievalOp)

  protected lazy val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistry)

  def logDeviceSeen(id: Device.Id): Directive0 = {
    extractRequestContext flatMap { _ =>
      onComplete {
        deviceRegistry.updateLastSeen(id)
      }
    } flatMap (_ => pass)
  }

  /**
    * An ota client PUT a list of packages to record they're installed on a vehicle, overwriting any previous such list.
    */
  def updateInstalledPackages(id: Device.Id): Route = {
    entity(as[List[PackageId]]) { ids =>
      val f = DeviceUpdates
        .update(id, ids, resolverClient, deviceRegistry)
        .map(_ => NoContent)

      complete(f)
    }
  }

  /**
    * An ota client GET which packages await installation for the given vehicle,
    * in the form a Seq of [[PendingUpdateRequest]]
    * whose order was specified via [[setInstallOrder]].
    * To actually download each binary file, [[downloadPackage]] is used.
    * <br>
    * Special case: For a vehicle whose installation queue is blocked,
    * no packages are returned.
    *
    * @see [[data.UpdateStatus]] (two of interest: InFlight and Pending)
    */
  def pendingPackages(device: Device.Id): Route = {
    import org.genivi.sota.core.data.client.PendingUpdateRequest._
    import ResponseConversions._

    val pendingIO =
      for {
        isBlkd  <- BlockedInstalls.isBlockedInstall(device)
        pending <- if (isBlkd) {
                     DBIO.successful(Seq.empty)
                   } else {
                     DeviceUpdates
                       .findPendingPackageIdsFor(device)
                       .map(_.toResponse)
                   }
      } yield pending

    complete(db.run(pendingIO))
  }

  /**
    * An ota client GET the binary file for the package that the given [[UpdateRequest]] and [[Device]] refers to.
    */
  def downloadPackage(device: Device.Id, updateId: Refined[String, Uuid]): Route = {
    withRangeSupport {
      complete(packageDownloadProcess.buildClientDownloadResponse(device, updateId))
    }
  }

  /**
    * An ota client POST for the given [[UpdateRequest]] an [[InstallReport]]
    * (describing the outcome after installing the package in question).
    */
  def reportInstall(updateId: Refined[String, Uuid]): Route = {
    entity(as[InstallReport]) { report =>
      val responseF =
        DeviceUpdates
          .buildReportInstallResponse(report.device, report.update_report)
      complete(responseF)
    }
  }

  /**
    * A web client fetches the results of updates to a given [[Device]].
    */
  def results(device: Device.Id): Route = {
    complete(db.run(OperationResults.byDevice(device)))
  }

  /**
    * A web client fetches the results of a given [[UpdateRequest]].
    */
  def resultsForUpdate(device: Device.Id, uuid: Refined[String, Uuid]): Route = {
    complete(db.run(OperationResults.byDeviceIdAndId(device, uuid)))
  }

  /**
    * An ota client POST a [[PackageId]] to schedule installation on a vehicle.
    * Internally an [[UpdateRequest]] and an [[UpdateSpec]] are persisted for that [[PackageId]].
    * Resolver is not contacted.
    */
  def queueDeviceUpdate(ns: Namespace, device: Device.Id): Route = {
    entity(as[PackageId]) { packageId =>
      val result = updateService.queueDeviceUpdate(ns, device, packageId)
      complete(result)
    }
  }

  def sync(device: Device.Id)
          (implicit s: Show[Device.Id]): Route = {
    val ttl = Instant.now.plus(5, ChronoUnit.MINUTES)
    // TODO: Config RVI destination path (or ClientServices.getpackages)
    // TODO: pass namespace
    connectivity.client.sendMessage(s"genivi.org/device/${s.show(device)}/sota/getpackages", io.circe.Json.Null, ttl)
    // TODO: Confirm getpackages in progress to vehicle?
    complete(NoContent)
  }

  /**
    * The web app PUT the order in which the given [[UpdateRequest]]s are to be installed on a vehicle.
    */
  def setInstallOrder(device: Device.Id): Route = {
    entity(as[Map[Int, UUID]]) { updateIds =>
      val sorted: List[UUID] = updateIds.toList.sortBy(_._1).map(_._2)
      val resp = DeviceUpdates.buildSetInstallOrderResponse(device, sorted)
      complete(resp)
    }
  }

  /**
    * The web app PUT to unblock the installation queue of a vehicle.
    */
  def unblockInstall(deviceId: Device.Id): Route = {
    val resp =
      db.run(BlockedInstalls.updateBlockedInstallQueue(deviceId, isBlocked = false))
        .map(_ => NoContent)

    complete(resp)
  }

  /**
    * The web app PUT the status of the given ([[UpdateSpec]], VIN) to [[UpdateStatus.Canceled]]
    */
  def cancelUpdate(deviceId: Device.Id, updateId: Refined[String, Uuid]): Route = {
    val response = db.run(UpdateSpecs.cancelUpdate(deviceId, updateId)).map {
      i: Int => i match {
          case 0 => HttpResponse(StatusCodes.BadRequest)
          case _ => HttpResponse(StatusCodes.NoContent)
        }
    }
    complete(response)
  }

  val route = {
    (pathPrefix("api" / "v1" / "vehicle_updates") & extractDeviceUuid) { device =>
      get {
        pathEnd { logDeviceSeen(device) { pendingPackages(device) } } ~
        path("queued") { pendingPackages(device) } ~
        path("results") { results(device) } ~
        (extractUuid & path("results")) { uuid => resultsForUpdate(device, uuid) } ~
        (extractUuid & path("download")) { uuid => downloadPackage(device, uuid) }
      } ~
      put {
        path("installed") { updateInstalledPackages(device) } ~
        path("order") { setInstallOrder(device) } ~
        (extractUuid & path("cancelupdate") ) { uuid => cancelUpdate(device, uuid) } ~
        path("unblock") { unblockInstall(device) }
      } ~
      post {
        path("sync") { sync(device) } ~
        (namespaceExtractor & pathEnd) { ns => queueDeviceUpdate(ns, device) } ~
        (extractUuid & pathEnd) { reportInstall }
      }
    }
  }
}
