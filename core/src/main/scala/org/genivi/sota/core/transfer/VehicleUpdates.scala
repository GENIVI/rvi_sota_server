/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer


import java.util.UUID

import akka.actor.ActorSystem
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.{HttpHeader, HttpResponse, StatusCodes}
import io.circe.Json
import io.circe.syntax._
import org.genivi.sota.data.Namespace._
import org.genivi.sota.core.common.NamespaceDirective
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.{InstallHistories, OperationResults, UpdateRequests, UpdateSpecs}
import org.genivi.sota.db.SlickExtensions
import slick.dbio.DBIO
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.db.UpdateSpecs._
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.core.rvi.UpdateReport

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import org.genivi.sota.refined.SlickRefined._

import scala.util.control.NoStackTrace


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

  def reportInstall(vin: Vehicle.Vin, updateReport: UpdateReport)
                   (implicit ec: ExecutionContext, db: Database): Future[UpdateSpec] = {
    val namespaceQuery = for {
      s <- updateSpecs.filter(r => r.requestId === updateReport.update_id)
    } yield s.namespace
    //TODO: get default namespace for now, this should be replaced with a lookup to the updates table eventually
    val namespace = NamespaceDirective.defaultNs.get
    val writeResultsIO = updateReport
      .operation_results
      .map(r => org.genivi.sota.core.data.OperationResult(r.id, updateReport.update_id, r.result_code, r.result_text,
                                                          vin, namespace))
      .map(r => OperationResults.persist(r))

    val dbIO = for {
      spec <- findUpdateSpecFor(vin, updateReport.update_id)
      _ <- DBIO.sequence(writeResultsIO)
      _ <- UpdateSpecs.setStatus(spec, UpdateStatus.Finished)
      _ <- InstallHistories.log(spec.namespace, vin, spec.request.id, spec.request.packageId, success = true)
    } yield spec.copy(status = UpdateStatus.Finished)

    db.run(dbIO)
  }

  def findPendingPackageIdsFor(vin: Vehicle.Vin)
                              (implicit db: Database, ec: ExecutionContext) : DBIO[Seq[UpdateRequest]] = {
    updateSpecs
      .filter(_.vin === vin)
      .filter(_.status.inSet(List(UpdateStatus.InFlight, UpdateStatus.Pending)))
      .join(updateRequests).on(_.requestId === _.id)
      .sortBy(r => (r._2.installPos.asc, r._2.creationTime.asc))
      .map(_._2)
      .result
  }

  def findUpdateSpecFor(vin: Vehicle.Vin, updateRequestId: UUID)
                       (implicit ec: ExecutionContext, db: Database): DBIO[UpdateSpec] = {
    updateSpecs
      .filter(_.vin === vin)
      .filter(_.requestId === updateRequestId)
      .join(updateRequests).on(_.requestId === _.id)
      .result
      .headOption
      .flatMap {
        case Some(((ns: Namespace, uuid: UUID, updateVin: Vehicle.Vin, status: UpdateStatus.UpdateStatus),
                   updateRequest: UpdateRequest)) =>
          val spec = UpdateSpec(updateRequest, updateVin, status, Set.empty[Package])
          DBIO.successful(spec)
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
      .flatMap { existingUr =>
        DBIO.sequence {
          existingUr.map { ur =>
            updateRequests
              .filter(_.id === ur)
              .map(_.installPos)
              .update(prios(ur))
              .map(_ => (ur, prios(ur)))
          }
        }
      }
      .transactionally
  }
}
