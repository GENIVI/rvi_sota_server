/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import java.util.UUID

import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import eu.timepit.refined.string.Regex
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.{Device, Namespace, PackageId, Uuid}
import org.genivi.sota.db.Operators._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.components.{Component, ComponentRepository}
import org.genivi.sota.resolver.data.Firmware
import org.genivi.sota.resolver.db.PackageIdDatabaseConversions._
import org.genivi.sota.resolver.filters._
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}


object DeviceRepository {
  import org.genivi.sota.db.SlickExtensions._
  import org.genivi.sota.refined.SlickRefined._

  class InstalledFirmwareTable(tag: Tag) extends Table[(Firmware, Uuid)](tag, "Firmware") {
    def namespace     = column[Namespace]           ("namespace")
    def module        = column[Firmware.Module]     ("module")
    def firmware_id   = column[Firmware.FirmwareId] ("firmware_id")
    def last_modified = column[Long]                ("last_modified")
    def device        = column[Uuid]                ("device_uuid")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("pk_installedFirmware", (namespace, module, firmware_id, device))

    def * = (namespace, module, firmware_id, last_modified, device).shaped <>
      (p => (Firmware(p._1, p._2, p._3, p._4), p._5),
        (fw: (Firmware, Uuid)) =>
          Some((fw._1.namespace, fw._1.module, fw._1.firmwareId, fw._1.lastModified, fw._2)))
  }
  // scalastyle:on

  val installedFirmware = TableQuery[InstalledFirmwareTable]

  def firmwareExists(namespace: Namespace, module: Firmware.Module)
                    (implicit ec: ExecutionContext): DBIO[Firmware.Module] = {
    val res = for {
      ifw <- installedFirmware.filter(i => i.namespace === namespace && i.module === module).result.headOption
    } yield ifw
    res.flatMap(_.fold[DBIO[Firmware.Module]]
      (DBIO.failed(Errors.MissingFirmware))(x => DBIO.successful(x._1.module)))
  }

  def installFirmware
  (namespace: Namespace, module: Firmware.Module, firmware_id: Firmware.FirmwareId,
   last_modified: Long, device: Uuid)
  (implicit ec: ExecutionContext): DBIO[Unit] = {
    for {
      _ <- firmwareExists(namespace, module)
      _ <- installedFirmware.insertOrUpdate((Firmware(namespace, module, firmware_id, last_modified), device))
    } yield()
  }

  def firmwareOnDevice
  (namespace: Namespace, deviceId: Uuid)
  (implicit ec: ExecutionContext): DBIO[Seq[Firmware]] = {
    installedFirmware
      .filter(_.namespace === namespace)
      .filter(_.device === deviceId)
      .result
      .map(_.map(_._1))
  }

  //This method is only intended to be called when the client reports installed firmware.
  //It therefore clears all installed firmware for the given vin and replaces with the reported
  //state instead.
  def updateInstalledFirmware(device: Uuid, firmware: Set[Firmware])
                             (implicit ec: ExecutionContext): DBIO[Unit] = {
    (for {
      _       <- installedFirmware.filter(_.device === device).delete
      _       <- installedFirmware ++= firmware.map(fw =>
        (Firmware(fw.namespace, fw.module, fw.firmwareId, fw.lastModified), device))
    } yield ()).transactionally
  }

  /*
   * Installed packages.
   */

  // scalastyle:off
  class InstalledPackageTable(tag: Tag) extends Table[(Uuid, UUID)](tag, "InstalledPackage") {
    def device = column[Uuid]("device_uuid")
    def packageUuid = column[UUID]("package_uuid")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("pk_installedPackage", (device, packageUuid))

    def * = (device, packageUuid).shaped <> (identity, (vp: (Uuid, UUID)) => Some(vp))
  }
  // scalastyle:on

  val installedPackages = TableQuery[InstalledPackageTable]

  def installPackage(namespace: Namespace, device: Uuid, pkgId: PackageId)
                    (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      pkg <- PackageRepository.exists(namespace, pkgId)
      _ <- installedPackages.insertOrUpdate((device, pkg.uuid))
    } yield ()

  def uninstallPackage(namespace: Namespace, device: Uuid, pkgId: PackageId)
                      (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      pkg <- PackageRepository.exists(namespace, pkgId)
      _ <- installedPackages.filter { ip => ip.device === device && ip.packageUuid === pkg.uuid }.delete
    } yield ()

  def updateInstalledPackages(namespace: Namespace, device: Uuid, packages: Set[PackageId] )
                             (implicit ec: ExecutionContext): DBIO[Unit] = {
    def filterAvailablePackages(ids: Set[UUID]) : DBIO[Set[UUID]] =
      PackageRepository.loadByUuids(ids).map(_.map(_.uuid).toSet)

    def deleteOld(deletedPackages: Set[UUID]) =
      installedPackages
        .filter(_.device === device)
        .filter(_.packageUuid.inSet(deletedPackages))
        .delete

    def insertNew(newPackages: Set[UUID]) = installedPackages ++= newPackages.map((device, _))

    val dbio = for {
      packageUuids <- PackageRepository.toPackageUuids(namespace, packages)
      installedPackages <- DeviceRepository.installedOn(device).map(_.map(_.uuid))
      newPackages = packageUuids -- installedPackages
      deletedPackages = installedPackages -- packageUuids
      newAvailablePackages <- filterAvailablePackages(newPackages)
      _ <- insertNew(newAvailablePackages)
      _ <- deleteOld(deletedPackages)
    } yield ()

    dbio.transactionally
  }

  def installedOn(device: Uuid, regexFilter: Option[String] = None)
                 (implicit ec: ExecutionContext) : DBIO[Set[Package]] = {
    installedPackages
      .join(PackageRepository.packages).on(_.packageUuid === _.uuid)
      .regexFilter(regexFilter)(_._2.name, _._2.version)
      .filter(_._1.device === device)
      .map { case (_, pkg) => pkg }
      .result
      .map(_.toSet)
  }

  def listInstalledPackages: DBIO[Seq[(Namespace, Uuid, PackageId)]] =
    installedPackages
      .join(PackageRepository.packages).on(_.packageUuid === _.uuid)
      .map { case (ip, p) => (p.namespace, ip.device, LiftedPackageId(p.name, p.version))}
      .result

  /*
   * Installed components.
   */

  // scalastyle:off
  class InstalledComponentTable(tag: Tag)
    extends Table[(Namespace, Uuid, Component.PartNumber)](tag, "InstalledComponent") {

    def namespace = column[Namespace]("namespace")
    def device = column[Uuid]("device_uuid")
    def partNumber = column[Component.PartNumber]("partNumber")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("pk_installedComponent", (namespace, device, partNumber))

    def * = (namespace, device, partNumber)
  }
  // scalastyle:on

  val installedComponents = TableQuery[InstalledComponentTable]

  def listInstalledComponents: DBIO[Seq[(Namespace, Uuid, Component.PartNumber)]] =
    installedComponents.result

  def deleteInstalledComponentById(namespace: Namespace, device: Uuid): DBIO[Int] =
    installedComponents.filter(i => i.namespace === namespace && i.device === device).delete

  def installComponent(namespace: Namespace, device: Uuid, part: Component.PartNumber)
                      (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- ComponentRepository.exists(namespace, part)
      _ <- installedComponents += ((namespace, device, part))
    } yield ()

  def uninstallComponent
  (namespace: Namespace, device: Uuid, part: Component.PartNumber)
  (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- ComponentRepository.exists(namespace, part)
      _ <- installedComponents.filter { ic =>
        ic.namespace  === namespace &&
          ic.device === device &&
          ic.partNumber === part
      }.delete
    } yield ()

  def componentsOnDevice(namespace: Namespace, device: Uuid)
                        (implicit ec: ExecutionContext): DBIO[Seq[Component.PartNumber]] = {
    installedComponents
      .filter(_.namespace === namespace)
      .filter(_.device === device)
      .map(_.partNumber)
      .result
  }

  def search(namespace : Namespace,
             re        : Option[Refined[String, Regex]],
             pkgName   : Option[PackageId.Name],
             pkgVersion: Option[PackageId.Version],
             part      : Option[Component.PartNumber],
             deviceRegistry: DeviceRegistry)
            (implicit db: Database, ec: ExecutionContext, mat: ActorMaterializer): Future[Seq[Uuid]] = {
    def toRegex[T](r: Refined[String, T]): Refined[String, Regex] =
      refineV[Regex](r.get).right.getOrElse(Refined.unsafeApply(".*"))

    val vins = re.fold[FilterAST](True)(VinMatches)

    val pkgs = (pkgName, pkgVersion) match {
      case (Some(re1), Some(re2)) => HasPackage(toRegex(re1), toRegex(re2))
      case _ => True
    }

    val comps = part.fold[FilterAST](True)(r => HasComponent(toRegex(r)))

    val filter = And(vins, And(pkgs, comps))

    for {
      devices <- deviceRegistry.listNamespace(namespace)
      searchResult <- DbDepResolver.filterDevices(namespace, devices.map(d => d.uuid -> d.deviceId).toMap, filter)
    } yield searchResult
  }

  def allInstalledPackagesById(namespace: Namespace, ids: Set[PackageId], devices: Set[Uuid])
                              (implicit db: Database, ec: ExecutionContext): Future[Map[Uuid, Seq[PackageId]]] = {
    val dbDevicesIO =
      installedPackages.join(PackageRepository.inSetQuery(ids)).on(_.packageUuid === _.uuid)
        .filter(_._2.namespace === namespace)
        .map { case (ip, pkg) => (ip.device, LiftedPackageId(pkg.name, pkg.version)) }
        .union(ForeignPackages.installedQuery(ids))
        .filter(_._1.inSet(devices))
        .result
        .map {
          _.foldLeft(Map.empty[Uuid, Seq[PackageId]]) { case (acc, (device, pid)) =>
            val all = pid +: acc.getOrElse(device, Seq.empty[PackageId])
            acc + (device -> all)
          }
        }

    db.run(dbDevicesIO)
  }
}
