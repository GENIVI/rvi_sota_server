/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer


import java.util.UUID

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import io.circe.syntax._
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.{InstallHistories, OperationResults, UpdateRequests, UpdateSpecs}
import org.genivi.sota.db.SlickExtensions
import slick.dbio.DBIO
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.db.UpdateSpecs._
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.core.rvi.UpdateReport

import scala.concurrent.{ExecutionContext, Future}
import org.genivi.sota.refined.SlickRefined._


object InstalledPackagesUpdate {
  import SlickExtensions._

  case class UpdateSpecNotFound(msg: String) extends Exception(msg)

  def update(vin: Vehicle.Vin, packageIds: List[PackageId], resolverClient: ExternalResolverClient): Future[Unit] = {
    val ids = packageIds.asJson
    resolverClient.setInstalledPackages(vin, ids)
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
    val writeResultsIO = updateReport
      .operation_results
      .map(r => org.genivi.sota.core.data.OperationResult(r.id, updateReport.update_id, r.result_code, r.result_text))
      .map(r => OperationResults.persist(r))

    val dbIO = for {
      spec <- findUpdateSpecFor(vin, updateReport.update_id)
      _ <- DBIO.sequence(writeResultsIO)
      _ <- UpdateSpecs.setStatus(spec, UpdateStatus.Finished)
      _ <- InstallHistories.log(spec.namespace, vin, spec.request.id, spec.request.packageId, success = true)
    } yield spec.copy(status = UpdateStatus.Finished)

    db.run(dbIO)
  }

  def findPendingPackageIdsFor(ns: Namespace, vin: Vehicle.Vin)
                              (implicit db: Database, ec: ExecutionContext) : DBIO[Seq[UpdateRequest]] = {
    updateSpecs
      .filter(r => r.namespace === ns && r.vin === vin)
      .filter(_.status.inSet(List(UpdateStatus.InFlight, UpdateStatus.Pending)))
      .join(updateRequests).on(_.requestId === _.id)
      .sortBy(_._2.creationTime.asc)
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
          val spec = UpdateSpec(ns, updateRequest, updateVin, status, Set.empty[Package])
          DBIO.successful(spec)
        case None =>
          DBIO.failed(
            UpdateSpecNotFound(s"Could not find an update request with id $updateRequestId for vin ${vin.get}")
          )
      }
  }
}
