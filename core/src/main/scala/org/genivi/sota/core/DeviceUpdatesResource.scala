/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import io.circe.Json
import java.util.UUID

import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.db.{BlockedInstalls, OperationResults, UpdateSpecs}
import org.genivi.sota.core.resolver.{Connectivity, DefaultConnectivity, ExternalResolverClient}
import org.genivi.sota.core.user_profile.UserProfile
import org.genivi.sota.core.rvi.{InstallReport, OperationResult, UpdateReport}
import org.genivi.sota.core.storage.PackageStorage
import java.time.Instant
import java.time.temporal.ChronoUnit

import org.genivi.sota.core.transfer.DeviceUpdates
import org.genivi.sota.core.data.UpdateSpec
import org.genivi.sota.core.transfer.{DefaultUpdateNotifier, PackageDownloadProcess}
import org.genivi.sota.data.{Namespace, PackageId, UpdateType, Uuid}
import slick.driver.MySQLDriver.api.Database
import cats.syntax.show.toShowOps
import org.genivi.sota.http.{AuthedNamespaceScope, Scopes}
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.core.data.client.PendingUpdateRequest._
import UpdateSpec._
import org.genivi.sota.messaging.Messages.BandwidthUsage
import org.genivi.sota.rest.ResponseConversions._
import MessageBusPublisher._

import scala.concurrent.Future

class DeviceUpdatesResource(db: Database,
                            resolverClient: ExternalResolverClient,
                            deviceRegistry: DeviceRegistry,
                            userProfile: Option[UserProfile],
                            authNamespace: Directive1[AuthedNamespaceScope],
                            messageBus: MessageBusPublisher)
                           (implicit system: ActorSystem, mat: ActorMaterializer,
                            connectivity: Connectivity = DefaultConnectivity) {

  import Directives._
  import org.genivi.sota.http.UuidDirectives._
  import org.genivi.sota.marshalling.CirceMarshallingSupport._
  import org.genivi.sota.http.ErrorHandler._

  implicit val ec = system.dispatcher
  implicit val _db = db
  implicit val _config = system.settings.config

  lazy val packageRetrievalOp = (new PackageStorage).retrieveResponse _

  lazy val packageDownloadProcess = new PackageDownloadProcess(db, packageRetrievalOp)

  protected lazy val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistry)

  def logDeviceSeen(id: Uuid): Directive0 = {
    val f = deviceRegistry.updateLastSeen(id)
    onComplete(f).flatMap(_ => pass)
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
    (entity(as[List[PackageId]]) & extractLog) { (ids, log) =>
      val f = DeviceUpdates
        .update(id, ids, resolverClient)
        .flatMap { _ =>
          //ignore failures here as the actual work has been done in update()
          deviceRegistry
            .updateLastSeen(id)
            .recover { case e =>
              log.error(e, "Could not update device last seen")
              Future.successful(())
            }
        }
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

  def downloadPackage(device: Uuid, updateId: Refined[String, Uuid.Valid]): Route =
    withRangeSupport {
      val userWithinLimitFuture =
        userProfile match {
          case Some(up) =>
            for {
              deviceData <- deviceRegistry.fetchMyDevice(device)
              withinLimit <- up.checkDeviceLimit(deviceData.namespace, device)
              result <- if (withinLimit) Future.successful(())
              else Future.failed(SotaCoreErrors.DeviceLimitReached)
            } yield result
          case None =>
            Future.successful(())
        }

      val f = userWithinLimitFuture
        .flatMap(_ => packageDownloadProcess.buildClientDownloadResponse(device, updateId))
        .pipeToBus(messageBus) { case (pkg, _) =>
          BandwidthUsage(UUID.randomUUID(), pkg.namespace, Instant.now, pkg.size, UpdateType.Package, pkg.id.show)
        }

      complete(f.map(_._2))
    }

  /**
    * An ota client POST for the given [[UpdateRequest]] an [[UpdateReport]]
    * (describing the outcome after installing the package in question).
    */
  def reportUpdateResult(device: Uuid, updateId: Uuid): Route = {
    entity(as[List[OperationResult]]) { results =>
      val responseF = DeviceUpdates
        .buildReportInstallResponse(device, UpdateReport(updateId.toJava, results), messageBus)
      complete(responseF)
    }
  }

  /**
    * An ota client POST for the given [[UpdateRequest]] an [[InstallReport]]
    * (describing the outcome after installing the package in question).
    *
    * Deprecated by reportUpdateResult for mydeviceRoutes.
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

  def authDeviceNamespace(deviceId: Uuid) : Directive1[AuthedNamespaceScope] =
    authNamespace flatMap { ns =>
      import scala.util.{Success, Failure}
      val f = deviceRegistry.fetchDevice(ns, deviceId)
      onComplete(f) flatMap {
        case Success(device) =>
          provide(ns)
        case Failure(t) => reject(failNamespaceRejection("Cannot validate namespace"))
      }
    }

  val routeDeprecated = handleErrors { authNamespace { authedNs =>
    // vehicle_updates is deprecated and will be removed sometime in the future
    (pathPrefix("api" / "v1") & ( pathPrefix("vehicle_updates") | pathPrefix("device_updates"))
                              & extractUuid) { device =>
      get {
        pathEnd {
          authedNs.oauthScopeReadonly(s"ota-core.${device.show}.read") {
            logDeviceSeen(device) { pendingPackages(device) }
          }
        } ~
        (path("queued") & authedNs.oauthScopeReadonly(s"ota-core.${device.show}.read")) {
          // Backward compatible with sota_client v0.2.17
          logDeviceSeen(device) { pendingPackages(device) }
        } ~
        (extractRefinedUuid & path("download")) { updateId =>
          authedNs.oauthScopeReadonly(s"ota-core.${device.show}.read") {
            downloadPackage(device, updateId)
          }
        } ~
        authDeviceNamespace(device) { ns =>
          path("queued") { pendingPackages(device) } ~
          path("blocked") { getBlockedInstall(device) } ~
          path("results") { results(device) } ~
          (extractRefinedUuid & path("results")) { updateId => resultsForUpdate(device, updateId) }
        }
      } ~
      put {
        path("installed") {
          authedNs.oauthScope(s"ota-core.${device.show}.write") {
            updateInstalledPackages(device)
          }
        } ~
        path("system_info") {
          authedNs.oauthScope(s"ota-core.${device.show}.write") {
            updateSystemInfo(device)
          }
        } ~
        authDeviceNamespace(device) { ns =>
          (extractRefinedUuid & path("cancelupdate")) { updateId => cancelUpdate(device, updateId) } ~
          path("order") { setInstallOrder(device) } ~
          path("blocked") { setBlockedInstall(device) }
        }
      } ~
      post {
        (extractRefinedUuid & pathEnd & authedNs.oauthScope(s"ota-core.${device.show}.write")) { reportInstall } ~
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
    }}
  }

  val apiRoutes = handleErrors {
    (pathPrefix("api" / "v1" / "device_updates") & extractUuid) { device =>
      authDeviceNamespace(device) { ns =>
        val scope = Scopes.devices(ns)
        scope.get {
          path("queued") { pendingPackages(device) } ~
          path("blocked") { getBlockedInstall(device) } ~
          path("results") { results(device) } ~
          (extractRefinedUuid & path("results")) { updateId => resultsForUpdate(device, updateId) }
        } ~
        scope.put {
          path("blocked") { setBlockedInstall(device) } ~
          path("order") { setInstallOrder(device) } ~
          (extractRefinedUuid & path("cancelupdate")) { updateId => cancelUpdate(device, updateId) }
        } ~
        scope.post {
          pathEnd { queueDeviceUpdate(ns, device) } ~
          path("sync") { sync(device) }
        } ~
        scope.delete {
          path("blocked") { deleteBlockedInstall(device) }
        }
      }
    }
  }

  val mydeviceRoutes = handleErrors { authNamespace { authedNs =>
    (pathPrefix("api" / "v1" / "mydevice") & extractUuid) { device =>
      pathPrefix("updates") {
        (get & authedNs.oauthScopeReadonly(s"ota-core.${device.show}.read")) {
          pathEnd {
            logDeviceSeen(device) { pendingPackages(device) }
          } ~
          (extractRefinedUuid & path("download")) { updateId =>
            downloadPackage(device, updateId)
          }
        } ~
        (post & authedNs.oauthScope(s"ota-core.${device.show}.write")) {
          (extractUuid & pathEnd ) { updateId =>
            reportUpdateResult(device, updateId)
          }
        }
      } ~
      (put & authedNs.oauthScope(s"ota-core.${device.show}.write")) {
        path("installed") {
          updateInstalledPackages(device)
        } ~
        path("system_info") {
          updateSystemInfo(device)
        }
      }
    }}
  }

  val route = apiRoutes ~ mydeviceRoutes ~ routeDeprecated
}
