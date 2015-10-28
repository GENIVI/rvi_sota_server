/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.genivi.sota.core.data.{Vehicle, Package, InstallHistory}
import org.joda.time.DateTime
import slick.driver.JdbcTypesComponent._
import slick.driver.MySQLDriver.api._


object InstallHistories {

  import Mappings._
  import org.genivi.sota.refined.SlickRefined._

  // scalastyle:off
  class InstallHistoryTable(tag: Tag) extends Table[InstallHistory](tag, "InstallHistory") {

    def id             = column[Long]           ("id", O.PrimaryKey, O.AutoInc)
    def vin            = column[Vehicle.Vin]    ("vin")
    def packageName    = column[Package.Name]   ("packageName")
    def packageVersion = column[Package.Version]("packageVersion")
    def success        = column[Boolean]        ("success")
    def completionTime = column[DateTime]       ("completionTime")

    def * = (id.?, vin, packageName, packageVersion, success, completionTime).shaped <>
      (r => InstallHistory(r._1, r._2, Package.Id(r._3, r._4), r._5, r._6),
        (h: InstallHistory) =>
          Some((h.id, h.vin, h.packageId.name, h.packageId.version, h.success, h.completionTime)))
  }
  // scalastyle:on

  val installHistories = TableQuery[InstallHistoryTable]

  def list(vin: Vehicle.Vin): DBIO[Seq[InstallHistory]] =
    installHistories.filter(_.vin === vin).result

  def log(vin: Vehicle.Vin, pkgId: Package.Id, success: Boolean): DBIO[Int] =
    installHistories += InstallHistory(None, vin, pkgId, success, DateTime.now)

}
