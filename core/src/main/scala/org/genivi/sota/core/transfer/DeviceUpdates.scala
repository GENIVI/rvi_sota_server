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
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.UpdateSpecs._
import org.genivi.sota.core.db._
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.core.rvi.UpdateReport
import org.genivi.sota.data.{Namespace, PackageId, UpdateStatus, Uuid}
import org.genivi.sota.db.SlickExtensions
import org.genivi.sota.http.Errors.MissingEntity
import org.genivi.sota.messaging.{MessageBusPublisher, Messages}
import slick.dbio.DBIO
import slick.driver.MySQLDriver.api._
import org.genivi.sota.refined.PackageIdDatabaseConversions._
import org.genivi.sota.data.UpdateStatus.UpdateStatus
import shapeless.{::, HNil}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

object DeviceUpdates {
  import SlickExtensions._
  import org.genivi.sota.marshalling.CirceInstances._
  import org.genivi.sota.refined.SlickRefined._

  case class SetOrderFailed(msg: String) extends Exception(msg) with NoStackTrace

  /**
    * Tell resolver to record the given packages as installed on a vehicle, overwriting any previous such list.
    */
  def update(device: Uuid,
             packageIds: List[PackageId],
             resolverClient: ExternalResolverClient)
            (implicit ec: ExecutionContext): Future[Unit] = {
    // TODO: core should be able to send this instead!
    val j = Json.obj("packages" -> packageIds.asJson, "firmware" -> Json.arr())
    resolverClient.setInstalledPackages(device, j)
  }

  def buildReportInstallResponse(device: Uuid, updateReport: UpdateReport,
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
  def reportInstall(device: Uuid, updateReport: UpdateReport, messageBus: MessageBusPublisher)
                   (implicit ec: ExecutionContext, db: Database): Future[UpdateSpec] = {

    // add a row (with fresh UUID) to OperationResult table for each result of this report
    val writeResultsIO = for {
      rviOpResult <- updateReport.operation_results
      dbOpResult   = OperationResult.from(
        rviOpResult,
        updateReport.update_id,
        device)
    } yield OperationResults.persist(dbOpResult)

    // look up the UpdateSpec to rewrite its status and to use it as FK in InstallHistory
    val newStatus = if (updateReport.isSuccess) UpdateStatus.Finished else UpdateStatus.Failed
    val dbIO = for {
      spec <- findUpdateSpecFor(device, updateReport.update_id)
      _    <- DBIO.sequence(writeResultsIO)
      _    <- UpdateSpecs.setStatus(spec, newStatus)
      _    <- InstallHistories.log(spec, updateReport.isSuccess)
      _    <- if (updateReport.isFail) {
                BlockedInstalls.persist(device)
              } else {
                DBIO.successful(true)
              }
      pkg <- findUpdateRequestPackage(updateReport.update_id)
    } yield (spec.copy(status = newStatus), pkg.namespace)

    db.run(dbIO.transactionally).andThen {
      case scala.util.Success((spec, namespace)) =>
        messageBus.publish(Messages.UpdateSpec(namespace, device, spec.request.packageUuid, spec.status, Instant.now))
    }.map(_._1)
  }

  private def findUpdateRequestPackage(updateRequestId: UUID)(implicit ec: ExecutionContext): DBIO[Package] = {
    UpdateRequests.byId(updateRequestId).flatMap(ur => Packages.byUuid(ur.packageUuid))
  }


  def findPendingPackageIdsFor(device: Uuid)
                              (implicit db: Database,
                               ec: ExecutionContext)
  : DBIO[Seq[(UpdateRequest, UpdateStatus :: PackageId :: Instant :: HNil)]] = {
    val statusToInclude = List(UpdateStatus.InFlight, UpdateStatus.Pending)

    val updateSpecsQ =
      updateSpecs
      .filter(_.device === device)
      .filter(_.status.inSet(statusToInclude))

    val updateRequestStatusQ = for {
        us <- updateSpecsQ
        ur <- updateRequests if ur.id === us.requestId
        pkg <- Packages.packages if ur.packageUuid === pkg.uuid
      } yield (pkg.namespace, ur, us, LiftedPackageId(pkg.name, pkg.version))

    val updateReqStatusPkgIdIO = updateRequestStatusQ
      .sortBy { case (ns, ur, us, pid) => (us.installPos.asc, us.creationTime.asc) }
      .result

    updateReqStatusPkgIdIO.flatMap {
      BlacklistedPackages.filterBlacklisted(p => (p._1, p._4))
    }.map { _.zipWithIndex.map {
      case ((ns, ur, us, pid), idx) => (ur.copy(installPos = idx), us.status :: pid :: us.updatedAt :: HNil) }
    }
  }


  /**
    * Find the [[UpdateSpec]]-s (including dependencies) whatever their [[UpdateStatus]]
    * for the given (device, [[UpdateRequest]]) combination.
    * <br>
    * Note: for pending ones see [[findPendingPackageIdsFor()]]
    */
  def findUpdateSpecFor(device: Uuid, updateRequestId: UUID)
                       (implicit ec: ExecutionContext, db: Database, s: Show[Uuid]): DBIO[UpdateSpec] = {
    val updateSpecsIO =
      updateSpecs
      .filter(_.device === device)
      .filter(_.requestId === updateRequestId)
      .join(updateRequests).on(_.requestId === _.id)
      .joinLeft(requiredPackages.filter(_.device === device)).on(_._1.requestId === _.requestId)
      .result

    val specsWithDepsIO = updateSpecsIO flatMap { specsWithDeps =>
      val dBIOActions = specsWithDeps map { case ((updateSpec, updateReq), requiredPO) =>
        (updateSpec.toUpdateSpec(updateReq, Set.empty), requiredPO)
      } map { case (spec, requiredPO) =>
        val depsIO = requiredPO map {
          case (_, _, packageUuid) =>
            Packages.byUuid(packageUuid).map(Some(_))
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

  def countQueuedBy(packageId: PackageId, namespace: Namespace): DBIO[Int] = {
    val query = for {
      us <- updateSpecs if us.status === UpdateStatus.Pending
      ur <- updateRequests if ur.id === us.requestId
      pkg <- Packages.packages if pkg.uuid === ur.packageUuid &&
      pkg.namespace === namespace && pkg.name === packageId.name && pkg.version === packageId.version
    } yield us.device

    query.countDistinct.result
  }

  /**
    * Arguments denote the number of [[UpdateRequest]] to be installed on a vehicle.
    * Each [[UpdateRequest]] may depend on a group of dependencies collected in an [[UpdateSpec]].
    *
    * @return All UpdateRequest UUIDs (for the vehicle in question) for which a pending UpdateSpec exists
    */
  private def findSpecsForSorting(device: Uuid, numUpdateRequests: Int)
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
  def buildSetInstallOrderResponse(device: Uuid, order: List[UUID])
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
  def persistInstallOrder(device: Uuid, order: List[UUID])
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
