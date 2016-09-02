/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.devices

import java.time.Instant

import org.genivi.sota.data.{Device, Namespace, PackageId}
import slick.driver.MySQLDriver.api._
import org.genivi.sota.db.Operators._
import scala.concurrent.ExecutionContext

object ForeignPackages {

  import org.genivi.sota.db.SlickExtensions._
  import org.genivi.sota.refined.SlickRefined._

  type InstalledForeignPkgRow = (Device.Id, PackageId.Name, PackageId.Version, Instant)

  case class InstalledForeignPackage(device: Device.Id, packageId: PackageId,
                                     lastModified: Instant)

  private def toTuple(fp: InstalledForeignPackage): Option[InstalledForeignPkgRow] =
    Some((fp.device, fp.packageId.name, fp.packageId.version, fp.lastModified))

  private def fromTuple(installedForeignPkgRow: InstalledForeignPkgRow): InstalledForeignPackage =
    installedForeignPkgRow match {
      case (device, name, version, lastModified) =>
        InstalledForeignPackage(device, PackageId(name, version), lastModified)
    }

  class InstalledForeignPackageTable(tag: Tag) extends Table[InstalledForeignPackage](tag, "InstalledForeignPackage") {
    def device = column[Device.Id]("device_uuid")
    def name = column[PackageId.Name]("name")
    def version = column[PackageId.Version]("version")
    def lastModified = column[Instant]("last_modified")

    def pk = primaryKey("pk_foreignInstalledPackage", (name, version, device))

    def * = (device, name, version, lastModified) <> (fromTuple, toTuple)
  }

  private val foreignPackages = TableQuery[InstalledForeignPackageTable]

  def setInstalled(device: Device.Id, packages: Set[PackageId])
                  (implicit ec: ExecutionContext): DBIO[Set[InstalledForeignPackage]] = {
    val now = Instant.now()

    def installedNow(nonForeign: Set[PackageId]): Set[InstalledForeignPackage] =
      (packages -- nonForeign).map(p => InstalledForeignPackage(device, p, now))

    val dbIO = for {
      deleteExisting <- foreignPackages.filter(_.device === device).delete
      nonForeignInstalled <- DeviceRepository.installedOn(device)
      installNow = installedNow(nonForeignInstalled)
      insertCount <- foreignPackages ++= installNow
    } yield installNow

    dbIO.transactionally
  }

  def installedOn(device: Device.Id, regexFilter: Option[String] = None)
                 (implicit ec: ExecutionContext): DBIO[Seq[PackageId]] = {
    foreignPackages
      .filter(_.device === device)
      .regexFilter(regexFilter)(_.name, _.version)
      .result.map(_.map(_.packageId))
  }
}
