/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.genivi.sota.core.data.InstallHistory
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.core.data.UpdateSpec
import java.time.Instant

import slick.driver.MySQLDriver.api._


object InstallHistories {

  import org.genivi.sota.db.SlickExtensions._
  import org.genivi.sota.refined.SlickRefined._

  /**
    * A log of update installs attempted on a VIN.
    * Each logged row includes the [[UpdateRequest]] identity, VIN, time of attempt
    * and whether the install was successful.
    */
  // scalastyle:off
  class InstallHistoryTable(tag: Tag) extends Table[InstallHistory](tag, "InstallHistory") {

    def id             = column[Long]             ("id", O.AutoInc)
    def namespace      = column[Namespace]        ("namespace")
    def vin            = column[Vehicle.Vin]      ("vin")
    def updateId       = column[java.util.UUID]   ("update_request_id")
    def packageName    = column[PackageId.Name]   ("packageName")
    def packageVersion = column[PackageId.Version]("packageVersion")
    def success        = column[Boolean]          ("success")
    def completionTime = column[Instant]          ("completionTime")

    // given `id` is already unique across namespaces, no need to include namespace. Also avoids Slick issue #966.
    def pk = primaryKey("pk_InstallHistoryTable", (id))

    def * = (id.?, namespace, vin, updateId, packageName, packageVersion, success, completionTime).shaped <>
      (r => InstallHistory(r._1, r._2, r._3, r._4, PackageId(r._5, r._6), r._7, r._8),
        (h: InstallHistory) =>
          Some((h.id, h.namespace, h.vin, h.updateId, h.packageId.name, h.packageId.version, h.success, h.completionTime)))
  }
  // scalastyle:on

  /**
   * Internal helper definition to access the SQL table
   */
  private val installHistories = TableQuery[InstallHistoryTable]

  /**
   * List the install attempts on a specific VIN, fetched from [[InstallHistoryTable]].
   *
   * @param vin The VIN to fetch data for
   * @return A list of the install history for that VIN
   */
  def list(ns: Namespace, vin: Vehicle.Vin): DBIO[Seq[InstallHistory]] =
    installHistories.filter(i => i.namespace === ns && i.vin === vin).result

  /**
   * Add a row (with auto-inc PK) to [[InstallHistoryTable]]
   * to persist the outcome of an [[UpdateSpec]] install attempt
   * as reported by the SOTA client via RVI.
   *
   * @param vin The VIN that the install attempt ran on
   * @param updateId The Id of the [[UpdateRequest]] that was attempted to be installed
   * @param success Whether the install was successful
   */
  def log(ns: Namespace, vin: Vehicle.Vin, updateId: java.util.UUID,
          packageId: PackageId, success: Boolean): DBIO[Int] = {
    installHistories += InstallHistory(None, ns, vin, updateId, packageId, success, Instant.now)
  }

  /**
    * Add a row (with auto-inc PK) to [[InstallHistoryTable]]
    * to persist the outcome of an [[UpdateSpec]] install attempt
    * as reported by the SOTA client via RVI.
    *
    * @param spec The ([[UpdateRequest]], VIN) combination whose install was attempted
    * @param success Whether the install was successful
    */
  def log(spec: UpdateSpec, success: Boolean): DBIO[Int] = {
    log(
      spec.namespace, spec.vin,
      spec.request.id, spec.request.packageId,
      success)
  }

}
