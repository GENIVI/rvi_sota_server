/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.device_registry.db

import java.time.Instant

import org.genivi.sota.data.{Namespace, PackageId, PaginatedResult, Uuid}
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

object InstalledPackages {

  import org.genivi.sota.db.SlickExtensions._
  import org.genivi.sota.refined.SlickRefined._

  type InstalledPkgRow = (Uuid, PackageId.Name, PackageId.Version, Instant)

  case class InstalledPackage(device: Uuid, packageId: PackageId,
                              lastModified: Instant)

  case class DevicesCount(deviceCount: Int, groupIds: Set[Uuid])

  private def toTuple(fp: InstalledPackage): Option[InstalledPkgRow] =
    Some((fp.device, fp.packageId.name, fp.packageId.version, fp.lastModified))

  private def fromTuple(installedForeignPkgRow: InstalledPkgRow): InstalledPackage =
    installedForeignPkgRow match {
      case (device, name, version, lastModified) =>
        InstalledPackage(device, PackageId(name, version), lastModified)
    }

  class InstalledPackageTable(tag: Tag) extends Table[InstalledPackage](tag, "InstalledPackage") {
    def device = column[Uuid]("device_uuid")
    def name = column[PackageId.Name]("name")
    def version = column[PackageId.Version]("version")
    def lastModified = column[Instant]("last_modified")

    def pk = primaryKey("pk_foreignInstalledPackage", (name, version, device))

    def * = (device, name, version, lastModified) <> (fromTuple, toTuple)
  }

  val defaultLimit = 50

  private val installedPackages = TableQuery[InstalledPackageTable]

  def setInstalled(device: Uuid, packages: Set[PackageId])
                  (implicit ec: ExecutionContext): DBIO[Unit] =
    DBIO.seq(
      installedPackages.filter(_.device === device).delete,
      installedPackages ++= packages.map(InstalledPackage(device, _, Instant.now()))
    ).transactionally

  def installedOn(device: Uuid)(implicit ec: ExecutionContext): DBIO[Seq[InstalledPackage]] =
    installedPackages
      .filter(_.device === device)
      .result

  def getDevicesCount(pkg: PackageId, ns: Namespace)(implicit ec: ExecutionContext): DBIO[DevicesCount] =
    for {
      devices <- installedPackages
        .filter(p => p.name === pkg.name && p.version === pkg.version)
        .join(DeviceRepository.devices).on(_.device === _.uuid)
        .filter(_._2.namespace === ns)
        .map(_._1.device)
        .countDistinct
        .result
      groups <- installedPackages
        .filter(p => p.name === pkg.name && p.version === pkg.version)
        .join(GroupMemberRepository.groupMembers).on(_.device === _.deviceUuid)
        .join(DeviceRepository.devices).on(_._2.deviceUuid === _.uuid)
        .filter(_._2.namespace === ns)
        .map(_._1._2.groupId)
        .distinct
        .result
    } yield DevicesCount(devices, groups.toSet)

  def getInstalledForAllDevices(ns: Namespace, moffset: Option[Long], mlimit: Option[Long])
                               (implicit ec: ExecutionContext): DBIO[PaginatedResult[PackageId]] = {
    val offset = moffset.getOrElse[Long](0)
    val limit = mlimit.getOrElse[Long](defaultLimit)
    val query = DeviceRepository
      .devices.filter(_.namespace === ns)
      .join(installedPackages).on(_.uuid === _.device)
      .map(r => (r._2.name, r._2.version))
      .distinct

    val pagedquery = query.paginateAndSort(identity, offset, limit)
    val pkgResult = pagedquery.result.map(_.map {case (name, version) => PackageId(name, version)})

    query.length.result.zip(pkgResult).map{ case (total, values) =>
      PaginatedResult(total=total, limit=limit, offset=offset, values=values)
    }
  }
}
