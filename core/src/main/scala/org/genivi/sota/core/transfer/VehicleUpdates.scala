/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer


import java.util.UUID

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import io.circe.Json
import io.circe.syntax._
import org.genivi.sota.core.data.UpdateStatus.UpdateStatus
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.{Packages, UpdateSpecs}
import org.genivi.sota.db.SlickExtensions
import slick.dbio.DBIO
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.db.UpdateSpecs._
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.core.rvi.UpdateReport

import scala.concurrent.{ExecutionContext, Future}
import org.genivi.sota.refined.SlickRefined._

import scala.util.control.NoStackTrace
import org.genivi.sota.core.db.OperationResults
import org.genivi.sota.core.db.InstallHistories
import java.time.Instant
import org.genivi.sota.data.PackageId.{Name, Version}
import org.genivi.sota.data.Vehicle.Vin


object VehicleUpdates {
  import SlickExtensions._

  case class UpdateSpecNotFound(msg: String) extends Exception(msg) with NoStackTrace
  case class SetOrderFailed(msg: String) extends Exception(msg) with NoStackTrace

  /**
    * Tell resolver to record the given packages as installed on a vehicle, overwriting any previous such list.
    */
  def update(vin: Vehicle.Vin, packageIds: List[PackageId], resolverClient: ExternalResolverClient): Future[Unit] = {
    // TODO: core should be able to send this instead!
    val j = Json.obj("packages" -> packageIds.asJson, "firmware" -> Json.arr())
    resolverClient.setInstalledPackages(vin, j)
  }

  def buildReportInstallResponse(vin: Vehicle.Vin, updateReport: UpdateReport)
                                (implicit ec: ExecutionContext, db: Database): Future[HttpResponse] = {
    reportInstall(vin, updateReport) map { _ =>
      HttpResponse(StatusCodes.NoContent)
    } recover { case t: UpdateSpecNotFound =>
      HttpResponse(StatusCodes.NotFound, entity = t.getMessage)
    }
  }

  /**
    * An [[UpdateReport]] describes what happened in the client for an [[UpdateRequest]]
    * <ul>
    *   <li>Persist in [[OperationResults.OperationResultTable]] each operation result
    *       for a combination of ([[UpdateRequest]], VIN)</li>
    *   <li>Persist the status of the [[UpdateSpec]] (Finished for update report success, Failed otherwise)</li>
    *   <li>Persist the outcome of the [[UpdateSpec]] as a whole in [[InstallHistories.InstallHistoryTable]]</li>
    * </ul>
    */
  def reportInstall(vin: Vehicle.Vin, updateReport: UpdateReport)
                   (implicit ec: ExecutionContext, db: Database): Future[UpdateSpec] = {

    // add a row (with fresh UUID) to OperationResult table for each result of this report
    val writeResultsIO = (ns: Namespace) => for {
      rviOpResult <- updateReport.operation_results
      dbOpResult   = OperationResult.from(
        rviOpResult,
        updateReport.update_id,
        vin,
        ns)
    } yield OperationResults.persist(dbOpResult)

    // look up the UpdateSpec to rewrite its status and to use it as FK in InstallHistory
    val newStatus = if (updateReport.isSuccess) UpdateStatus.Finished else UpdateStatus.Failed
    val dbIO = for {
      spec <- findUpdateSpecFor(vin, updateReport.update_id)
      _    <- DBIO.sequence(writeResultsIO(spec.namespace))
      _    <- UpdateSpecs.setStatus(spec, newStatus)
      _    <- InstallHistories.log(spec, updateReport.isSuccess)
      _    <- cancelInstallationQueueIO(vin, spec, updateReport.isFail, updateReport.update_id)
    } yield spec.copy(status = newStatus)

    db.run(dbIO.transactionally)
  }

  /**
    * <ul>
    *   <li>
    *     For a failed UpdateReport, mark as cancelled the rest of the installation queue.
    *     <ul>
    *       <li>"The rest of the installation queue" defined as those (InFlight and Pending) UpdateSpec-s
    *       coming after the given one, for the VIN in question.</li>
    *       <li>Note: those UpdateSpec-s correspond to different UpdateRequests than the current one.</li>
    *     </ul>
    *   </li>
    *   <li>For a successful UpdateReport, do nothing.</li>
    * </ul>
    */
  def cancelInstallationQueueIO(vin: Vehicle.Vin,
                                spec: UpdateSpec,
                                isFail: Boolean,
                                updateRequestId: UUID): DBIO[Int] = {
    updateSpecs
      .filter(_.vin === vin && isFail)
      .filter(_.requestId =!= updateRequestId)
      .filter(_.status.inSet(List(UpdateStatus.InFlight, UpdateStatus.Pending)))
      .filter(us =>
        (us.installPos > spec.installPos) ||
        (us.installPos === spec.installPos && us.creationTime > spec.creationTime)
      )
      .map(_.status)
      .update(UpdateStatus.Canceled)
  }

  /**
    * Find the [[UpdateRequest]]-s for:
    * <ul>
    *   <li>the given VIN</li>
    *   <li>and for which [[UpdateSpec]]-s exist with Pending or InFlight [[UpdateStatus]]</li>
    * </ul>
    */
  def findPendingPackageIdsFor(vin: Vehicle.Vin)
                              (implicit db: Database, ec: ExecutionContext) : DBIO[Seq[UpdateRequest]] = {
    updateSpecs
      .filter(_.vin === vin)
      .filter(_.status.inSet(List(UpdateStatus.InFlight, UpdateStatus.Pending)))
      .join(updateRequests).on(_.requestId === _.id)
      .sortBy { case (sp, _) => (sp.installPos.asc, sp.creationTime.asc) }
      .map    { case (_, ur) => ur }
      .result
      .map { _.zipWithIndex.map { case (ur, idx) => ur.copy(installPos = idx) } }
  }

  /**
    * Find the [[UpdateSpec]]-s (including dependencies) whatever their [[UpdateStatus]]
    * for the given ([[UpdateRequest]], VIN) combination.
    * <br>
    * Note: for pending ones see [[findPendingPackageIdsFor()]]
    */
  def findUpdateSpecFor(vin: Vehicle.Vin, updateRequestId: UUID)
                       (implicit ec: ExecutionContext, db: Database): DBIO[UpdateSpec] = {

    val updateSpecsIO =
      updateSpecs
      .filter(_.vin === vin)
      .filter(_.requestId === updateRequestId)
      .join(updateRequests).on(_.requestId === _.id)
      .joinLeft(requiredPackages).on(_._1.requestId === _.requestId)
      .result

    val specsWithDepsIO = updateSpecsIO flatMap { specsWithDeps =>
      val dBIOActions = specsWithDeps map { case ((updateSpec, updateReq), requiredPO) =>
        val (_, _, vin, status, installPos, creationTime) = updateSpec
        (UpdateSpec(updateReq, vin, status, Set.empty, installPos, creationTime), requiredPO)
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
          UpdateSpecNotFound(s"Could not find an update request with id $updateRequestId for vin ${vin.get}")
        )
    }
  }

  /**
    * Arguments denote the number of [[UpdateRequest]] to be installed on a vehicle.
    * Each [[UpdateRequest]] may depend on a group of dependencies collected in an [[UpdateSpec]].
    *
    * @return All UpdateRequest UUIDs (for the vehicle in question) for which a pending UpdateSpec exists
    */
  private def findSpecsForSorting(vin: Vehicle.Vin, numUpdateRequests: Int)
                                 (implicit ec: ExecutionContext): DBIO[Seq[UUID]] = {

    // all UpdateRequest UUIDs (for the vehicle in question) for which a pending UpdateSpec exists
    val q = updateRequests
      .join(updateSpecs).on(_.id === _.requestId)
      .filter(_._2.vin === vin)
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
  def buildSetInstallOrderResponse(vin: Vehicle.Vin, order: List[UUID])
                                  (implicit db: Database, ec: ExecutionContext): Future[HttpResponse] = {
    db.run(persistInstallOrder(vin, order))
      .map(_ => HttpResponse(StatusCodes.NoContent))
      .recover {
        case SetOrderFailed(msg) =>
          HttpResponse(StatusCodes.BadRequest, headers = Nil, msg)
      }
  }

  /**
    * Arguments denote a list of [[UpdateRequest]] to be installed on a vehicle in the order given.
    */
  def persistInstallOrder(vin: Vehicle.Vin, order: List[UUID])
                         (implicit ec: ExecutionContext): DBIO[Seq[(UUID, Int)]] = {
    val prios = order.zipWithIndex.toMap

    findSpecsForSorting(vin, order.size)
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
