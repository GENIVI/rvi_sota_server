/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer

import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import cats.Show
import io.circe.Json
import io.circe.syntax._
import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.core.data.UpdateStatus.UpdateStatus
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.UpdateSpecs._
import org.genivi.sota.core.db._
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.core.rvi.UpdateReport
import org.genivi.sota.data.{Device, Namespace, PackageId}
import org.genivi.sota.db.Operators._
import org.genivi.sota.db.SlickExtensions
import org.genivi.sota.http.Errors.MissingEntity
import org.genivi.sota.messaging.{MessageBusPublisher, Messages}
import slick.dbio.DBIO
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.util.control.NoStackTrace

object DeviceUpdates {
  import SlickExtensions._
  import org.genivi.sota.marshalling.CirceInstances._

  case class SetOrderFailed(msg: String) extends Exception(msg) with NoStackTrace

  /**
    * Tell resolver to record the given packages as installed on a vehicle, overwriting any previous such list.
    */
  def update(device: Device.Id,
             packageIds: List[PackageId],
             resolverClient: ExternalResolverClient)
            (implicit ec: ExecutionContext): Future[Unit] = {
    // TODO: core should be able to send this instead!
    val j = Json.obj("packages" -> packageIds.asJson, "firmware" -> Json.arr())
    resolverClient.setInstalledPackages(device, j)
  }

  def buildReportInstallResponse(device: Device.Id, updateReport: UpdateReport,
                                 messageBus: MessageBusPublisher)
                                (implicit ec: ExecutionContext, db: Database): Future[HttpResponse] = {
    reportInstall(device, updateReport, messageBus) map { _ =>
      HttpResponse(StatusCodes.NoContent)
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
  def reportInstall(device: Device.Id, updateReport: UpdateReport, messageBus: MessageBusPublisher)
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
                BlockedInstalls.persist(device)
              } else {
                DBIO.successful(true)
              }
    } yield spec.copy(status = newStatus)

    db.run(dbIO.transactionally).andThen {
      case scala.util.Success(spec) =>
        messageBus.publish(Messages.UpdateSpec(spec.namespace, device, spec.request.packageId,
        spec.status.toString))
    }
  }

  def findPendingPackageIdsFor(device: Device.Id, includeInFlight: Boolean = true)
                              (implicit db: Database,
                               ec: ExecutionContext): DBIO[Seq[(UpdateRequest, UpdateStatus, Instant)]] = {
    val statusToInclude =
      if(includeInFlight)
        List(UpdateStatus.InFlight, UpdateStatus.Pending)
      else
        List(UpdateStatus.Pending)

    updateSpecs
      .filter(_.device === device)
      .filter(_.status.inSet(statusToInclude))
      .join(updateRequests).on(_.requestId === _.id)
      .sortBy { case (sp, _) => (sp.installPos.asc, sp.creationTime.asc) }
      .map(r => (r._2, r._1.status, r._1.updateTime))
      .result
      .flatMap {
        BlacklistedPackages.filterBlacklisted[(UpdateRequest, UpdateStatus, Instant)] {
          case (ur, _, _) => (ur.namespace, ur.packageId)
        }
      }
      .map { _.zipWithIndex.map { case ((ur, us, updateAt), idx) => (ur.copy(installPos = idx), us, updateAt) } }
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
        val (_, _, deviceId, status, installPos, creationTime, updateTime) = updateSpec
        (UpdateSpec(updateReq, deviceId, status, Set.empty, installPos, creationTime, updateTime), requiredPO)
      } map { case (spec, requiredPO) =>
        val depsIO = requiredPO map {
          case (namespace, _, _, packageName, packageVersion) =>
            Packages
              .byId(namespace, PackageId(packageName, packageVersion))
              .map(Some(_))
        } getOrElse DBIO.successful(None)

        depsIO map { p => spec.copy(dependencies = p.toSet) }
      }

      DBIO.sequence(dBIOActions) map { specs =>
        val deps = specs.flatMap(_.dependencies).toSet
        specs.headOption.map(_.copy(dependencies = deps))
      }
    }

    specsWithDepsIO.failIfNone(MissingEntity(classOf[UpdateSpec]))
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
