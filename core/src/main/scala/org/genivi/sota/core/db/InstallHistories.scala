/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.genivi.sota.core.data.{InstallHistory, UpdateSpec}
import java.time.Instant
import java.util.UUID

import org.genivi.sota.core.db.UpdateSpecs.{UpdateSpecRow, UpdateSpecTable}
import org.genivi.sota.data.{Namespace, PackageId, UpdateStatus, Uuid}
import org.genivi.sota.http.Errors
import shapeless._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext


/**
 * Database mapping definition for the InstallHistory table.
 * This provides a history of update installs that have been attempted on a
 * device. It records the identity of the update, thi device, the time of the attempt
 * and whether the install was successful
 */
object InstallHistories {

  import org.genivi.sota.db.SlickExtensions._
  import org.genivi.sota.refined.PackageIdDatabaseConversions._
  import org.genivi.sota.refined.SlickRefined._
  import UpdateSpecs.UpdateStatusColumn

  /**
    * A log of update installs attempted on a device.
    * Each logged row includes the [[UpdateRequest]] identity, device id, time of attempt
    * and whether the install was successful.
    */
  // scalastyle:off
  class InstallHistoryTable(tag: Tag) extends Table[InstallHistory](tag, "InstallHistory") {

    def id             = column[Long]             ("id", O.AutoInc)
    def device         = column[Uuid]             ("device_uuid")
    def updateId       = column[java.util.UUID]   ("update_request_id")
    def packageUuid    = column[UUID]             ("package_uuid")
    def success        = column[Boolean]          ("success")
    def completionTime = column[Instant]          ("completionTime")

    // given `id` is already unique across namespaces, no need to include namespace. Also avoids Slick issue #966.
    def pk = primaryKey("pk_InstallHistoryTable", id)

    def tuple = (id.?, device, updateId, packageUuid, success, completionTime)

    def * = tuple.shaped <>
      (r => InstallHistory(r._1, r._2, r._3, r._4, r._5, r._6),
        (h: InstallHistory) =>
          Some((h.id, h.device, h.updateId, h.packageUuid, h.success, h.completionTime)))
  }
  // scalastyle:on

  /**
   * Internal helper definition to access the SQL table
   */
  private val installHistories = TableQuery[InstallHistoryTable]

  def deviceNamespace(device: Uuid)(implicit ec: ExecutionContext): DBIO[Namespace] = {
    installHistories
      .filter(_.device === device)
      .join(Packages.packages).on(_.packageUuid === _.uuid)
      .map(_._2)
      .map(_.namespace)
      .take(1)
      .result
      .failIfNotSingle(Errors.MissingEntity(classOf[InstallHistory]))
  }

  /**
   * List the install attempts that have been made on a specific device
   * This information is fetched from the InstallHistory SQL table.
   *
   * @param device The device to fetch data for
   * @return A list of the install history for that device
   */
  def list(device: Uuid)
          (implicit ec: ExecutionContext): DBIO[Seq[(InstallHistory, PackageId :: Boolean :: HNil)]] = {
    installHistories
      .filter(_.device === device)
      .join(Packages.packages).on(_.packageUuid === _.uuid)
      .joinLeft(UpdateSpecs.updateSpecs).on((ih, us) => ih._1.updateId === us.requestId && ih._1.device === us.device)
      .map { case ((ih, pkg), us) => (ih, LiftedPackageId(pkg.name, pkg.version), us.map(_.status)) }
      .result
      .map { _.map { case (ih, pkgId, status) => (ih, pkgId :: status.contains(UpdateStatus.Canceled) :: HNil) } }
  }

  def log(device: Uuid, updateId: java.util.UUID,
          packageUUid: UUID, success: Boolean): DBIO[Int] = {
    installHistories += InstallHistory(None, device, updateId, packageUUid, success, Instant.now)
  }

  /**
    * Add a row (with auto-inc PK) to [[InstallHistoryTable]]
    * to persist the outcome of an [[UpdateSpec]] install attempt
    * as reported by the SOTA client via RVI.
    *
    * @param spec The ([[UpdateRequest]], device) combination whose install was attempted
    * @param success Whether the install was successful
    */
  def log(spec: UpdateSpec, success: Boolean): DBIO[Int] = {
    log(spec.device, spec.request.id, spec.request.packageUuid, success)
  }

  protected [db] def logAll(updateSpecQuery: Query[UpdateSpecTable, UpdateSpecRow, Seq],
                            success: Boolean): DBIO[Int] = {
    val query = for {
      us <- updateSpecQuery
      ur <- UpdateRequests.all if ur.id === us.requestId
    } yield (Option.empty[Long], us.device, us.requestId, ur.packageUuid, success, Instant.now)

    installHistories.map(_.tuple).forceInsertQuery(query)
  }
}
