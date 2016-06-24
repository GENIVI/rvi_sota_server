/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.util.FastFuture
import cats.Show
import eu.timepit.refined.api.Refined
import io.circe.Json
import io.circe.syntax._
import org.genivi.sota.core.data.UpdateStatus.UpdateStatus
import java.util.UUID

import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.Packages
import org.genivi.sota.core.db.OperationResults
import org.genivi.sota.core.db.InstallHistories
import org.genivi.sota.core.db.UpdateSpecs
import org.genivi.sota.core.db.UpdateSpecs._
import org.genivi.sota.core.db.BlockedInstalls
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.core.rvi.UpdateReport
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{Device, PackageId}
import org.genivi.sota.db.SlickExtensions

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace
import slick.dbio.DBIO
import slick.driver.MySQLDriver.api._
import cats.syntax.show._

object DeviceUpdates {
  import SlickExtensions._
  import org.genivi.sota.refined.SlickRefined._
  import org.genivi.sota.marshalling.CirceInstances._

  case class UpdateSpecNotFound(msg: String) extends Exception(msg) with NoStackTrace
  case class SetOrderFailed(msg: String) extends Exception(msg) with NoStackTrace

  /**
    * Tell resolver to record the given packages as installed on a vehicle, overwriting any previous such list.
    */
  def update(device: Device.Id,
             packageIds: List[PackageId],
             resolverClient: ExternalResolverClient,
             deviceRegistry: DeviceRegistry)
            (implicit ec: ExecutionContext): Future[Unit] = {
    // TODO: core should be able to send this instead!
    val j = Json.obj("packages" -> packageIds.asJson, "firmware" -> Json.arr())
    deviceRegistry.fetchDevice(device).map { d => d.deviceId match {
      case Some(deviceId) =>
        // TODO: validation
        resolverClient.setInstalledPackages(Refined.unsafeApply(deviceId.underlying), j)
      case None => FastFuture.successful(())
    }}
  }

  def buildReportInstallResponse(device: Device.Id, updateReport: UpdateReport)
                                (implicit ec: ExecutionContext, db: Database): Future[HttpResponse] = {
    reportInstall(device, updateReport) map { _ =>
      HttpResponse(StatusCodes.NoContent)
    } recover { case t: UpdateSpecNotFound =>
      HttpResponse(StatusCodes.NotFound, entity = t.getMessage)
    }
  }

  /**
    * An [[UpdateReport]] describes what happened in the client for an [[UpdateRequest]]
    * <ul>
    *   <li>Persist in [[OperationResults.OperationResultTable]] each operation result
    *       for a combination of ([[UpdateRequest]], device)</li>
    *   <li>Persist the status of the [[UpdateSpec]] (Finished for update report success, Failed otherwise)</li>
    *   <li>Persist the outcome of the [[UpdateSpec]] as a whole in [[InstallHistories.InstallHistoryTable]]</li>
    * </ul>
    */
  def reportInstall(device: Device.Id, updateReport: UpdateReport)
                   (implicit ec: ExecutionContext, db: Database): Future[UpdateSpec] = {

    // add a row (with fresh UUID) to OperationResult table for each result of this report
    val writeResultsIO = (ns: Namespace) => for {
      rviOpResult <- updateReport.operation_results
      dbOpResult   = OperationResult.from(
        rviOpResult,
        updateReport.update_id,
        device,
        ns)
    } yield OperationResults.persist(dbOpResult)

    // look up the UpdateSpec to rewrite its status and to use it as FK in InstallHistory
    val newStatus = if (updateReport.isSuccess) UpdateStatus.Finished else UpdateStatus.Failed
    val dbIO = for {
      spec <- findUpdateSpecFor(device, updateReport.update_id)
      _    <- DBIO.sequence(writeResultsIO(spec.namespace))
      _    <- UpdateSpecs.setStatus(spec, newStatus)
      _    <- InstallHistories.log(spec, updateReport.isSuccess)
      _    <- if (updateReport.isFail) {
                BlockedInstalls.updateBlockedInstallQueue(device, isBlocked = true)
              } else {
                DBIO.successful(true)
              }
    } yield spec.copy(status = newStatus)

    db.run(dbIO.transactionally)
  }

  /**
    * Find the [[UpdateRequest]]-s for:
    * <ul>
    *   <li>the given device</li>
    *   <li>and for which [[UpdateSpec]]-s exist with Pending or InFlight [[UpdateStatus]]</li>
    * </ul>
    */
  def findPendingPackageIdsFor(device: Device.Id)
                              (implicit db: Database, ec: ExecutionContext) : DBIO[Seq[UpdateRequest]] = {
    updateSpecs
      .filter(_.device === device)
      .filter(_.status.inSet(List(UpdateStatus.InFlight, UpdateStatus.Pending)))
      .join(updateRequests).on(_.requestId === _.id)
      .sortBy { case (sp, _) => (sp.installPos.asc, sp.creationTime.asc) }
      .map    { case (_, ur) => ur }
      .result
      .map { _.zipWithIndex.map { case (ur, idx) => ur.copy(installPos = idx) } }
  }

  /**
    * Find the [[UpdateSpec]]-s (including dependencies) whatever their [[UpdateStatus]]
    * for the given (device, [[UpdateRequest]]) combination.
    * <br>
    * Note: for pending ones see [[findPendingPackageIdsFor()]]
    */
  def findUpdateSpecFor(device: Device.Id, updateRequestId: UUID)
                       (implicit ec: ExecutionContext, db: Database, s: Show[Device.Id]): DBIO[UpdateSpec] = {
    val updateSpecsIO =
      updateSpecs
      .filter(_.device === device)
      .filter(_.requestId === updateRequestId)
      .join(updateRequests).on(_.requestId === _.id)
      .joinLeft(requiredPackages).on(_._1.requestId === _.requestId)
      .result

    val specsWithDepsIO = updateSpecsIO flatMap { specsWithDeps =>
      val dBIOActions = specsWithDeps map { case ((updateSpec, updateReq), requiredPO) =>
        val (_, _, deviceId, status, installPos, creationTime) = updateSpec
        (UpdateSpec(updateReq, deviceId, status, Set.empty, installPos, creationTime), requiredPO)
      } map { case (spec, requiredPO) =>
        val depsIO = requiredPO map { case (namespace, _, _, packageName, packageVersion) =>
          Packages.byId(namespace, PackageId(packageName, packageVersion))
        } getOrElse DBIO.successful(None)

        depsIO map { p => spec.copy(dependencies = p.toSet) }
      }

      DBIO.sequence(dBIOActions) map { specs =>
        val deps = specs.flatMap(_.dependencies).toSet
        specs.headOption.map(_.copy(dependencies = deps))
      }
    }

    specsWithDepsIO flatMap {
      case Some(us) => DBIO.successful(us)
      case None =>
        DBIO.failed(
          UpdateSpecNotFound(s"Could not find an update request with id $updateRequestId for device ${device.show}")
        )
    }
  }

  /**
    * Arguments denote the number of [[UpdateRequest]] to be installed on a vehicle.
    * Each [[UpdateRequest]] may depend on a group of dependencies collected in an [[UpdateSpec]].
    *
    * @return All UpdateRequest UUIDs (for the vehicle in question) for which a pending UpdateSpec exists
    */
  private def findSpecsForSorting(device: Device.Id, numUpdateRequests: Int)
                                 (implicit ec: ExecutionContext): DBIO[Seq[UUID]] = {

    // all UpdateRequest UUIDs (for the vehicle in question) for which a pending UpdateSpec exists
    val q = updateRequests
      .join(updateSpecs).on(_.id === _.requestId)
      .filter(_._2.device === device)
      .filter(_._2.status === UpdateStatus.Pending)
      .map(_._1.id)

     q.result
      .flatMap { r =>
        if(r.size > numUpdateRequests) {
          DBIO.failed(SetOrderFailed("To set install order, all updates for a vehicle need to be specified"))
        } else if(r.size != numUpdateRequests) {
          DBIO.failed(SetOrderFailed("To set install order, all updates for a vehicle need to be pending"))
        } else {
          DBIO.successful(r)
        }
      }
  }

  /**
    * Arguments denote a list of [[UpdateRequest]] to be installed on a vehicle in the order given.
    */
  def buildSetInstallOrderResponse(device: Device.Id, order: List[UUID])
                                  (implicit db: Database, ec: ExecutionContext): Future[HttpResponse] = {
    db.run(persistInstallOrder(device, order))
      .map(_ => HttpResponse(StatusCodes.NoContent))
      .recover {
        case SetOrderFailed(msg) =>
          HttpResponse(StatusCodes.BadRequest, headers = Nil, msg)
      }
  }

  /**
    * Arguments denote a list of [[UpdateRequest]] to be installed on a vehicle in the order given.
    */
  def persistInstallOrder(device: Device.Id, order: List[UUID])
                         (implicit ec: ExecutionContext): DBIO[Seq[(UUID, Int)]] = {
    val prios = order.zipWithIndex.toMap

    findSpecsForSorting(device, order.size)
      .flatMap { existingUpdReqs =>
        DBIO.sequence {
          existingUpdReqs.map { ur =>
            updateSpecs
              .filter(_.requestId === ur)
              .map(_.installPos)
              .update(prios(ur))
              .map(_ => (ur, prios(ur)))
          }
        }
      }
      .transactionally
  }
}
