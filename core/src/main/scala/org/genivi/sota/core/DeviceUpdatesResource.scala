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
import io.circe.generic.auto._
import io.circe.Json
import java.util.UUID

import org.genivi.sota.common.DeviceRegistry
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
import org.genivi.sota.data.{Device, Namespace, PackageId, Uuid}

import scala.language.implicitConversions
import slick.driver.MySQLDriver.api.Database
import cats.syntax.show.toShowOps
import org.genivi.sota.http.AuthDirectives.AuthScope
import org.genivi.sota.messaging.Messages.DeviceSeen
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.core.data.client.PendingUpdateRequest._
import UpdateSpec._
import org.genivi.sota.rest.ResponseConversions._

class DeviceUpdatesResource(db: Database,
                            resolverClient: ExternalResolverClient,
                            deviceRegistry: DeviceRegistry,
                            authNamespace: Directive1[Namespace],
                            authDirective: AuthScope => Directive0,
                            messageBus: MessageBusPublisher)
                           (implicit system: ActorSystem, mat: ActorMaterializer,
                            connectivity: Connectivity = DefaultConnectivity) {

  import shapeless._
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

  def logDeviceSeen(id: Uuid): Directive0 = {
    extractRequestContext flatMap { _ =>
      onComplete {
        for {
          _ <- messageBus.publishSafe(DeviceSeen(id, Instant.now()))
          _ <- deviceRegistry.updateLastSeen(id)
        } yield ()
      }
    } flatMap (_ => pass)
  }

  def updateSystemInfo(id: Uuid): Route = {
    entity(as[Json]) { json =>
      complete(deviceRegistry.updateSystemInfo(id,json))
    }
  }

  /**
    * An ota client PUT a list of packages to record they're installed on a device, overwriting any previous such list.
    */
  def updateInstalledPackages(id: Uuid): Route = {
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
  def pendingPackages(device: Uuid): Route = {
    val vehiclePackages =
      DeviceUpdates.findPendingPackageIdsFor(device).map(_.toResponse)

    complete(db.run(vehiclePackages))
  }

  /**
    * An ota client GET the binary file for the package that the given [[UpdateRequest]] and [[Device]] refers to.
    */
  def downloadPackage(device: Uuid, updateId: Refined[String, Uuid.Valid]): Route = {
    withRangeSupport {
      complete(packageDownloadProcess.buildClientDownloadResponse(device, updateId))
    }
  }

  /**
    * An ota client POST for the given [[UpdateRequest]] an [[InstallReport]]
    * (describing the outcome after installing the package in question).
    */
  def reportInstall(updateId: Refined[String, Uuid.Valid]): Route = {
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
  def results(device: Uuid): Route = {
    complete(db.run(OperationResults.byDevice(device)))
  }

  /**
    * A web app fetches the results of a given (device, [[UpdateRequest]]) combination.
    */
  def resultsForUpdate(device: Uuid, update: Refined[String, Uuid.Valid]): Route = {
    complete(db.run(OperationResults.byDeviceIdAndId(device, update)))
  }

  /**
    * An ota client POST a [[PackageId]] to schedule installation on a device.
    * Internally an [[UpdateRequest]] and an [[UpdateSpec]] are persisted for that [[PackageId]].
    * Resolver is not contacted.
    */
  def queueDeviceUpdate(ns: Namespace, device: Uuid): Route = {
    entity(as[PackageId]) { packageId =>
      val result = updateService.queueDeviceUpdate(ns, device, packageId).map { case (ur, us, updateTime) =>
        ur.toResponse((us.status, packageId, updateTime))
      }
      complete(result)
    }
  }

  def sync(device: Uuid): Route = {
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
  def setInstallOrder(device: Uuid): Route = {
    entity(as[Map[Int, UUID]]) { updateIds =>
      val sorted: List[UUID] = updateIds.toList.sortBy(_._1).map(_._2)
      val resp = DeviceUpdates.buildSetInstallOrderResponse(device, sorted)
      complete(resp)
    }
  }

  /**
    * The web app GET whether the installation queue of the given device is blocked.
    */
  def getBlockedInstall(device: Uuid): Route = {
    complete(db.run(BlockedInstalls.get(device)))
  }

  /**
    * The web app PUT to block the installation queue of the given device.
    */
  def setBlockedInstall(device: Uuid): Route = {
    val resp = db.run(BlockedInstalls.persist(device))
      .map(_ => NoContent)
      complete(resp)
  }

  /**
    * The web app DELETE to unblock the installation queue of the given device.
    */
  def deleteBlockedInstall(device: Uuid): Route = {
    val resp = db.run(BlockedInstalls.delete(device))
      .map(_ => NoContent)
      complete(resp)
  }

  /**
    * The web app PUT the status of the given ([[UpdateSpec]], device) to [[UpdateStatus.Canceled]]
    */
  def cancelUpdate(device: Uuid, updateId: Refined[String, Uuid.Valid]): Route = {
    val response = db.run(UpdateSpecs.cancelUpdate(device, updateId)).map(_ => StatusCodes.NoContent)
    complete(response)
  }

  private[this] def failNamespaceRejection(msg: String): Rejection = AuthorizationFailedRejection

  def authDeviceNamespace(deviceId: Uuid) : Directive1[Namespace] =
    authNamespace flatMap { ns =>
      import scala.util.{Success, Failure}
      val f = deviceRegistry.fetchDevice(deviceId)
      onComplete(f) flatMap {
        case Success(device) =>
          if (device.namespace == ns) {
            provide(ns)
          } else {
            reject(AuthorizationFailedRejection)
          }
        case Failure(t) => reject(failNamespaceRejection("Cannot validate namespace"))
      }
    }

  val route = handleErrors {
    // vehicle_updates is deprecated and will be removed sometime in the future
    (pathPrefix("api" / "v1") & ( pathPrefix("vehicle_updates") | pathPrefix("device_updates"))
                              & extractDeviceUuid) { device =>
      get {
        pathEnd {
          authDirective(s"ota-core.${device.show}.read") {
            logDeviceSeen(device) { pendingPackages(device) }
          }
        } ~
        (path("queued") & authDirective(s"ota-core.${device.show}.read")) {
          // Backward compatible with sota_client v0.2.17
          logDeviceSeen(device) { pendingPackages(device) }
        } ~
        (extractUuid & path("download")) { updateId =>
          authDirective(s"ota-core.${device.show}.read") {
            downloadPackage(device, updateId)
          }
        } ~
        authDeviceNamespace(device) { ns =>
          path("queued") { pendingPackages(device) } ~
          path("blocked") { getBlockedInstall(device) } ~
          path("results") { results(device) } ~
          (extractUuid & path("results")) { updateId => resultsForUpdate(device, updateId) }
        }
      } ~
      put {
        path("installed") {
          authDirective(s"ota-core.${device.show}.write") {
            updateInstalledPackages(device)
          }
        } ~
        path("system_info") {
          authDirective(s"ota-core.${device.show}.write") {
            updateSystemInfo(device)
          }
        } ~
        authDeviceNamespace(device) { ns =>
          (extractUuid & path("cancelupdate")) { updateId => cancelUpdate(device, updateId) } ~
          path("order") { setInstallOrder(device) } ~
          path("blocked") { setBlockedInstall(device) }
        }
      } ~
      post {
        (extractUuid & pathEnd & authDirective(s"ota-core.${device.show}.write")) { reportInstall } ~
        authDeviceNamespace(device) { ns =>
          pathEnd { queueDeviceUpdate(ns, device) } ~
          path("sync") { sync(device) }
        }
      } ~
      delete {
        authDeviceNamespace(device) { ns =>
          path("blocked") { deleteBlockedInstall(device) }
        }
      }
    }
  }
}
