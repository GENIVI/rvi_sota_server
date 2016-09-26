/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.db

import java.time.Instant

import org.genivi.sota.data.{Device, Namespace, PackageId, Uuid}
import org.genivi.sota.db.Operators._
import slick.driver.MySQLDriver.api._
import org.genivi.sota.resolver.db.PackageIdDatabaseConversions._

import scala.concurrent.ExecutionContext

object ForeignPackages {

  import org.genivi.sota.refined.SlickRefined._
  import org.genivi.sota.db.SlickExtensions._

  type InstalledForeignPkgRow = (Uuid, PackageId.Name, PackageId.Version, Instant)

  case class InstalledForeignPackage(device: Uuid, packageId: PackageId,
                                     lastModified: Instant)

  private def toTuple(fp: InstalledForeignPackage): Option[InstalledForeignPkgRow] =
    Some((fp.device, fp.packageId.name, fp.packageId.version, fp.lastModified))

  private def fromTuple(installedForeignPkgRow: InstalledForeignPkgRow): InstalledForeignPackage =
    installedForeignPkgRow match {
      case (device, name, version, lastModified) =>
        InstalledForeignPackage(device, PackageId(name, version), lastModified)
    }

  class InstalledForeignPackageTable(tag: Tag) extends Table[InstalledForeignPackage](tag, "InstalledForeignPackage") {
    def device = column[Uuid]("device_uuid")
    def name = column[PackageId.Name]("name")
    def version = column[PackageId.Version]("version")
    def lastModified = column[Instant]("last_modified")

    def pk = primaryKey("pk_foreignInstalledPackage", (name, version, device))

    def * = (device, name, version, lastModified) <> (fromTuple, toTuple)
  }

  private val foreignPackages = TableQuery[InstalledForeignPackageTable]

  def setInstalled(device: Uuid, packages: Set[PackageId])
                  (implicit ec: ExecutionContext): DBIO[Set[InstalledForeignPackage]] = {
    val now = Instant.now()

    def installedNow(nonForeign: Set[PackageId]): Set[InstalledForeignPackage] =
      (packages -- nonForeign).map(p => InstalledForeignPackage(device, p, now))

    def installedKnown(device: Uuid): DBIO[Set[PackageId]] =
      DeviceRepository.installedOn(device).map(_.map(_.id).toSet)

    val dbIO = for {
      deleteExisting <- foreignPackages.filter(_.device === device).delete
      nonForeignInstalled <- installedKnown(device)
      installNow = installedNow(nonForeignInstalled)
      insertCount <- foreignPackages ++= installNow
    } yield installNow

    dbIO.transactionally
  }

  def installedOn(device: Uuid, regexFilter: Option[String] = None)
                 (implicit ec: ExecutionContext): DBIO[Seq[PackageId]] = {
    foreignPackages
      .filter(_.device === device)
      .regexFilter(regexFilter)(_.name, _.version)
      .result.map(_.map(_.packageId))
  }

  //scalastyle: off
  private def inSetQuery(ids: Set[PackageId]): Query[InstalledForeignPackageTable, InstalledForeignPackage, Seq] = {
    foreignPackages
      .filter { pkg =>
        (pkg.name.mappedTo[String] ++ pkg.version.mappedTo[String]).inSet(ids.map(id => id.name.get + id.version.get))
      }
  }
  //scalastyle: on

  protected [db] def installedQuery(ids: Set[PackageId])
  : Query[(Rep[Uuid], LiftedPackageId), (Uuid, PackageId), Seq] = {
    inSetQuery(ids).map(row => (row.device, LiftedPackageId(row.name, row.version)))
  }
}
