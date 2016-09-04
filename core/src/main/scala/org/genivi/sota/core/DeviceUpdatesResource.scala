/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
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
import io.circe.generic.auto._
import io.circe.Json
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
import org.genivi.sota.data.Namespace

import scala.language.implicitConversions
import slick.driver.MySQLDriver.api.Database
import cats.syntax.show.toShowOps
import org.genivi.sota.http.AuthDirectives.AuthScope
import org.genivi.sota.messaging.Messages.DeviceSeen
import org.genivi.sota.messaging.MessageBusPublisher

class DeviceUpdatesResource(db: Database,
                            resolverClient: ExternalResolverClient,
                            deviceRegistry: DeviceRegistry,
                            authNamespace: Directive1[Namespace],
                            authDirective: AuthScope => Directive0,
                            messageBus: MessageBusPublisher)
                           (implicit system: ActorSystem, mat: ActorMaterializer,
                            connectivity: Connectivity = DefaultConnectivity) {

  import Directives._
  import WebService._
  import org.genivi.sota.marshalling.CirceMarshallingSupport._
  import Device._
  import org.genivi.sota.http.ErrorHandler._

  implicit val ec = system.dispatcher
  implicit val _db = db
  implicit val _config = system.settings.config

  lazy val packageRetrievalOp = (new PackageStorage).retrieveResponse _

  lazy val packageDownloadProcess = new PackageDownloadProcess(db, packageRetrievalOp)

  protected lazy val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistry)

  def logDeviceSeen(id: Device.Id): Directive0 = {
    extractRequestContext flatMap { _ =>
      onComplete {
        for {
          _ <- messageBus.publishSafe(DeviceSeen(id, Instant.now()))
          _ <- deviceRegistry.updateLastSeen(id)
        } yield ()
      }
    } flatMap (_ => pass)
  }

  def updateSystemInfo(id: Device.Id): Route = {
    entity(as[Json]) { json =>
      complete(deviceRegistry.updateSystemInfo(id,json))
    }
  }

  /**
    * An ota client PUT a list of packages to record they're installed on a device, overwriting any previous such list.
    */
  def updateInstalledPackages(id: Device.Id): Route = {
    entity(as[List[PackageId]]) { ids =>
      val f = DeviceUpdates
        .update(id, ids, resolverClient)
        .map(_ => OK)

      complete(f)
    }
  }

  /**
    * An ota client GET which packages await installation for the given device,
    * in the form a Seq of [[PendingUpdateRequest]]
    * whose order was specified via [[setInstallOrder]].
    * To actually download each binary file, [[downloadPackage]] is used.
    * <br>
    * Special case: For a device whose installation queue is blocked,
    * no packages are returned.
    *
    * @see [[data.UpdateStatus]] (two of interest: InFlight and Pending)
    */
  def pendingPackages(device: Device.Id, includeInFlight: Boolean = true): Route = {
    import org.genivi.sota.core.data.client.PendingUpdateRequest._
    import ResponseConversions._
    import UpdateSpec._

    val vehiclePackages =
      DeviceUpdates
        .findPendingPackageIdsFor(device, includeInFlight)
        .map(_.toResponse)

    complete(db.run(vehiclePackages))
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
          .buildReportInstallResponse(report.device, report.update_report, messageBus)
      complete(responseF)
    }
  }

  /**
    * A web app fetches the results of updates to a given [[Device]].
    */
  def results(device: Device.Id): Route = {
    complete(db.run(OperationResults.byDevice(device)))
  }

  /**
    * A web app fetches the results of a given (device, [[UpdateRequest]]) combination.
    */
  def resultsForUpdate(device: Device.Id, uuid: Refined[String, Uuid]): Route = {
    complete(db.run(OperationResults.byDeviceIdAndId(device, uuid)))
  }

  /**
    * An ota client POST a [[PackageId]] to schedule installation on a device.
    * Internally an [[UpdateRequest]] and an [[UpdateSpec]] are persisted for that [[PackageId]].
    * Resolver is not contacted.
    */
  def queueDeviceUpdate(ns: Namespace, device: Device.Id): Route = {
    entity(as[PackageId]) { packageId =>
      val result = updateService.queueDeviceUpdate(ns, device, packageId)
      complete(result)
    }
  }

  def sync(device: Device.Id): Route = {
    val ttl = Instant.now.plus(5, ChronoUnit.MINUTES)
    // TODO: Config RVI destination path (or ClientServices.getpackages)
    // TODO: pass namespace
    connectivity.client.sendMessage(s"genivi.org/device/${device.show}/sota/getpackages", io.circe.Json.Null, ttl)
    // TODO: Confirm getpackages in progress to vehicle?
    complete(NoContent)
  }

  /**
    * The web app PUT the order in which the given [[UpdateRequest]]s are to be installed on the given device.
    */
  def setInstallOrder(device: Device.Id): Route = {
    entity(as[Map[Int, UUID]]) { updateIds =>
      val sorted: List[UUID] = updateIds.toList.sortBy(_._1).map(_._2)
      val resp = DeviceUpdates.buildSetInstallOrderResponse(device, sorted)
      complete(resp)
    }
  }

  /**
    * The web app GET whether the installation queue of the given device is blocked.
    */
  def getBlockedInstall(deviceId: Device.Id): Route = {
    complete(db.run(BlockedInstalls.get(deviceId)))
  }

  /**
    * The web app PUT to block the installation queue of the given device.
    */
  def setBlockedInstall(deviceId: Device.Id): Route = {
    val resp = db.run(BlockedInstalls.persist(deviceId))
      .map(_ => NoContent)
      complete(resp)
  }

  /**
    * The web app DELETE to unblock the installation queue of the given device.
    */
  def deleteBlockedInstall(deviceId: Device.Id): Route = {
    val resp = db.run(BlockedInstalls.delete(deviceId))
      .map(_ => NoContent)
      complete(resp)
  }

  /**
    * The web app PUT the status of the given ([[UpdateSpec]], device) to [[UpdateStatus.Canceled]]
    */
  def cancelUpdate(deviceId: Device.Id, updateId: Refined[String, Uuid]): Route = {
    val response = db.run(UpdateSpecs.cancelUpdate(deviceId, updateId)).map {
      case 0 => HttpResponse(StatusCodes.BadRequest)
      case _ => HttpResponse(StatusCodes.NoContent)
    }
    complete(response)
  }

  val route = handleErrors {
    // vehicle_updates is deprecated and will be removed sometime in the future
    (pathPrefix("api" / "v1") & ( pathPrefix("vehicle_updates") | pathPrefix("device_updates"))
                              & extractDeviceUuid) { device =>
      get {
        pathEnd { logDeviceSeen(device) { pendingPackages(device, includeInFlight = false) } } ~
        path("queued") { pendingPackages(device) } ~
        path("blocked") { getBlockedInstall(device) } ~
        path("results") { results(device) } ~
        (extractUuid & path("results")) { updateId => resultsForUpdate(device, updateId) } ~
        (extractUuid & path("download")) { updateId => downloadPackage(device, updateId) }
      } ~
        put {
          path("installed") {
            authDirective(s"ota-core.${device.show}.write") {
              updateInstalledPackages(device)
            }
          } ~
            path("system_info"){ updateSystemInfo(device) } ~
            path("order") { setInstallOrder(device) } ~
            (extractUuid & path("cancelupdate") & authNamespace) { (updateId, _) => cancelUpdate(device, updateId) } ~
            path("blocked") { setBlockedInstall(device) }
        } ~
        post {
          path("sync") { sync(device) } ~
            (authNamespace & pathEnd) { ns => queueDeviceUpdate(ns, device) } ~
            (extractUuid & pathEnd) { reportInstall }
        } ~
        delete {
          path("blocked") { deleteBlockedInstall(device) }
        }
    }
  }
}
