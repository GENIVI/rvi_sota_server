/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.vehicles

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.components.{Component, ComponentRepository}
import org.genivi.sota.resolver.data.Firmware
import org.genivi.sota.resolver.filters.FilterAST.{parseValidFilter, query}
import org.genivi.sota.resolver.filters.{And, FilterAST, HasComponent, HasPackage, True, VinMatches}
import org.genivi.sota.resolver.packages.{Package, PackageFilterRepository, PackageRepository}
import org.genivi.sota.resolver.resolve.ResolveFunctions
import org.joda.time._
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext


object VehicleRepository {

  // scalastyle:off
  class VinTable(tag: Tag) extends Table[Vehicle](tag, "Vehicle") {
    def namespace = column[Namespace]("namespace")
    def vin = column[Vehicle.Vin]("vin")

    def * = (namespace, vin) <> (Vehicle.fromVin, Vehicle.toVin)

    def pk = primaryKey("vin", (namespace, vin))  // insertOrUpdate doesn't work if
                                                  // we use O.PrimaryKey in the vin
                                                  // column, see Slick issue #966.
  }
  // scalastyle:on

  val vehicles = TableQuery[VinTable]

  def add(vehicle: Vehicle): DBIO[Int] =
    vehicles.insertOrUpdate(vehicle)

  def list: DBIO[Seq[Vehicle]] =
    vehicles.result

  def exists(namespace: Namespace, vin: Vehicle.Vin)(implicit ec: ExecutionContext): DBIO[Vehicle] =
    vehicles
      .filter(i => i.namespace === namespace && i.vin === vin)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[Vehicle]](DBIO.failed(Errors.MissingVehicle))(DBIO.successful))

  def delete(namespace: Namespace, vin: Vehicle.Vin): DBIO[Int] =
    vehicles.filter(i => i.namespace === namespace && i.vin === vin).delete

  def deleteVin(namespace: Namespace, vin: Vehicle.Vin)
               (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- VehicleRepository.exists(namespace, vin)
      _ <- deleteInstalledPackageByVin(namespace, vin)
      _ <- deleteInstalledComponentByVin(namespace, vin)
      _ <- delete(namespace, vin)
    } yield ()

  /*
   * Installed firmware.
   */

  // scalastyle:off
  class InstalledFirmwareTable(tag: Tag) extends Table[(Firmware, Vehicle.Vin)](tag, "Firmware") {

    def namespace     = column[Namespace]           ("namespace")
    def module        = column[Firmware.Module]     ("module")
    def firmware_id   = column[Firmware.FirmwareId] ("firmware_id")
    def last_modified = column[DateTime]            ("last_modified")
    def vin           = column[Vehicle.Vin]         ("vin")

    def pk = primaryKey("pk_installedFirmware", (namespace, module, firmware_id, vin))

    def * = (namespace, module, firmware_id, last_modified, vin).shaped <>
      (p => (Firmware(p._1, p._2, p._3, p._4), p._5),
        (fw: (Firmware, Vehicle.Vin)) => Some((fw._1.namespace, fw._1.module, fw._1.firmwareId, fw._1.lastModified, fw._2)))
  }
  // scalastyle:on

  val installedFirmware = TableQuery[InstalledFirmwareTable]

  def firmwareExists(namespace: Namespace, module: Firmware.Module)
                    (implicit ec: ExecutionContext): DBIO[Firmware.Module] = {
    val res = for {
      ifw <- installedFirmware.filter(i => i.namespace === namespace && i.module === module).result.headOption
    } yield ifw
    res.flatMap(_.fold[DBIO[Firmware.Module]]
      (DBIO.failed(Errors.MissingFirmwareException))(x => DBIO.successful(x._1.module)))
  }

  def installFirmware
    (namespace: Namespace, module: Firmware.Module, firmware_id: Firmware.FirmwareId,
     last_modified: DateTime, vin: Vehicle.Vin)
    (implicit ec: ExecutionContext): DBIO[Unit] = {
    for {
      _ <- exists(namespace, vin)
      _ <- firmwareExists(namespace, module)
      _ <- installedFirmware.insertOrUpdate((Firmware(namespace, module, firmware_id, last_modified), vin))
    } yield()
  }

  def firmwareOnVin
    (namespace: Namespace, vin: Vehicle.Vin)
    (implicit ec: ExecutionContext): DBIO[Seq[Firmware]] = {
    for {
      _  <- VehicleRepository.exists(namespace, vin)
      ps <- installedFirmware.filter(i => i.namespace === namespace && i.vin === vin).result
    } yield ps.map(_._1)
  }

  /*
   * Installed packages.
   */

  // scalastyle:off
  class InstalledPackageTable(tag: Tag) extends Table[(Namespace, Vehicle.Vin, PackageId)](tag, "InstalledPackage") {

    def namespace      = column[Namespace]        ("namespace")
    def vin            = column[Vehicle.Vin]      ("vin")
    def packageName    = column[PackageId.Name]   ("packageName")
    def packageVersion = column[PackageId.Version]("packageVersion")

    def pk = primaryKey("pk_installedPackage", (namespace, vin, packageName, packageVersion))

    def * = (namespace, vin, packageName, packageVersion).shaped <>
      (p => (p._1, p._2, PackageId(p._3, p._4)),
      (vp: (Namespace, Vehicle.Vin, PackageId)) => Some((vp._1, vp._2, vp._3.name, vp._3.version)))
  }
  // scalastyle:on

  val installedPackages = TableQuery[InstalledPackageTable]

  def installPackage
    (namespace: Namespace, vin: Vehicle.Vin, pkgId: PackageId)
    (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- exists(namespace, vin)
      _ <- PackageRepository.exists(namespace, pkgId)
      _ <- installedPackages.insertOrUpdate((namespace, vin, pkgId))
    } yield ()

  def uninstallPackage
    (namespace: Namespace, vin: Vehicle.Vin, pkgId: PackageId)
    (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- exists(namespace, vin)
      _ <- PackageRepository.exists(namespace, pkgId)
      _ <- installedPackages.filter {ip =>
             ip.namespace      === namespace &&
             ip.vin            === vin &&
             ip.packageName    === pkgId.name &&
             ip.packageVersion === pkgId.version
           }.delete
    } yield ()

  def updateInstalledPackages(namespace: Namespace, vin: Vehicle.Vin, packages: Set[PackageId] )
                             (implicit ec: ExecutionContext): DBIO[Unit] = {

    def filterAvailablePackages( ids: Set[PackageId] ) : DBIO[Set[PackageId]] =
      PackageRepository.load(namespace, ids).map(_.map(_.id))

    def helper( vehicle: Vehicle, newPackages: Set[PackageId], deletedPackages: Set[PackageId] )
                               (implicit ec: ExecutionContext) : DBIO[Unit] = DBIO.seq(
      installedPackages.filter( ip =>
        ip.namespace === namespace &&
        ip.vin === vehicle.vin &&
        (ip.packageName.mappedTo[String] ++ ip.packageVersion.mappedTo[String])
          .inSet( deletedPackages.map( id => id.name.get + id.version.get ))
      ).delete,
      installedPackages ++= newPackages.map((namespace, vehicle.vin, _))
    ).transactionally

    for {
      vehicle           <- VehicleRepository.exists(namespace, vin)
      installedPackages <- VehicleRepository.installedOn(namespace, vin)
      newPackages       =  packages -- installedPackages
      deletedPackages   =  installedPackages -- packages
      newAvailablePackages <- filterAvailablePackages(newPackages)
      _                 <- helper(vehicle, newAvailablePackages, deletedPackages)
    } yield ()
  }

  def installedOn(namespace: Namespace, vin: Vehicle.Vin)
                 (implicit ec: ExecutionContext) : DBIO[Set[PackageId]] =
    installedPackages.filter(i => i.namespace === namespace && i.vin === vin).result.map(_.map( _._3).toSet)

  def listInstalledPackages: DBIO[Seq[(Namespace, Vehicle.Vin, PackageId)]] =
    installedPackages.result
    // TODO: namespaces?

  def deleteInstalledPackageByVin(namespace: Namespace, vin: Vehicle.Vin): DBIO[Int] =
    installedPackages.filter(i => i.namespace === namespace && i.vin === vin).delete

  def packagesOnVinMap
    (namespace: Namespace)
    (implicit ec: ExecutionContext)
      : DBIO[Map[Vehicle.Vin, Seq[PackageId]]] =
    listInstalledPackages
      .map(_
        .filter(_._1 == namespace)
        .sortBy(_._2)
        .groupBy(_._2)
        .mapValues(_.map(_._3)))
    // TODO: namespaces?

  def packagesOnVin
    (namespace: Namespace, vin: Vehicle.Vin)
    (implicit ec: ExecutionContext): DBIO[Seq[PackageId]] =
    for {
      _  <- VehicleRepository.exists(namespace, vin)
      ps <- packagesOnVinMap(namespace)
              .map(_
                .get(vin)
                .toList
                .flatten)
    } yield ps

  /*
   * Installed components.
   */

  // scalastyle:off
  class InstalledComponentTable(tag: Tag)
      extends Table[(Namespace, Vehicle.Vin, Component.PartNumber)](tag, "InstalledComponent") {

    def namespace  = column[Namespace]           ("namespace")
    def vin        = column[Vehicle.Vin]         ("vin")
    def partNumber = column[Component.PartNumber]("partNumber")

    def pk = primaryKey("pk_installedComponent", (namespace, vin, partNumber))

    def * = (namespace, vin, partNumber)
  }
  // scalastyle:on

  val installedComponents = TableQuery[InstalledComponentTable]

  def listInstalledComponents: DBIO[Seq[(Namespace, Vehicle.Vin, Component.PartNumber)]] =
    installedComponents.result

  def deleteInstalledComponentByVin(namespace: Namespace, vin: Vehicle.Vin): DBIO[Int] =
    installedComponents.filter(i => i.namespace === namespace && i.vin === vin).delete

  def installComponent(namespace: Namespace, vin: Vehicle.Vin, part: Component.PartNumber)
                      (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- VehicleRepository.exists(namespace, vin)
      _ <- ComponentRepository.exists(namespace, part)
      _ <- installedComponents += ((namespace, vin, part))
    } yield ()

  def uninstallComponent(namespace: Namespace, vin: Vehicle.Vin, part: Component.PartNumber): DBIO[Int] =
    ???

  def componentsOnVinMap
    (namespace: Namespace)
    (implicit ec: ExecutionContext): DBIO[Map[Vehicle.Vin, Seq[Component.PartNumber]]] =
    VehicleRepository.listInstalledComponents
      .map(_
        .filter(_._1 == namespace)
        .sortBy(_._2)
        .groupBy(_._2)
        .mapValues(_.map(_._3)))

  def componentsOnVin(namespace: Namespace, vin: Vehicle.Vin)
                     (implicit ec: ExecutionContext): DBIO[Seq[Component.PartNumber]] =
    for {
      _  <- exists(namespace, vin)
      cs <- componentsOnVinMap(namespace)
              .map(_
                .get(vin)
                .toList
                .flatten)
    } yield cs

  def vinsWithPackagesAndComponents
    (namespace: Namespace)
    (implicit ec: ExecutionContext)
      : DBIO[Seq[(Vehicle, (Seq[PackageId], Seq[Component.PartNumber]))]] =
    for {
      vs   <- VehicleRepository.list
      ps   : Seq[Seq[PackageId]]
           <- DBIO.sequence(vs.map(v => VehicleRepository.packagesOnVin(namespace, v.vin)))
      cs   : Seq[Seq[Component.PartNumber]]
           <- DBIO.sequence(vs.map(v => VehicleRepository.componentsOnVin(namespace, v.vin)))
      vpcs : Seq[(Vehicle, (Seq[PackageId], Seq[Component.PartNumber]))]
           =  vs.zip(ps.zip(cs))
    } yield vpcs
    // TODO: namespaces?

  /*
   * Searching
   */

  def search(namespace : Namespace,
             re        : Option[Refined[String, Regex]],
             pkgName   : Option[PackageId.Name],
             pkgVersion: Option[PackageId.Version],
             part      : Option[Component.PartNumber])
            (implicit ec: ExecutionContext): DBIO[Seq[Vehicle]] = {

    def toRegex[T](r: Refined[String, T]): Refined[String, Regex] =
      Refined.unsafeApply(r.get)

    val vins  = re.fold[FilterAST](True)(VinMatches(_))
    val pkgs  = (pkgName, pkgVersion) match
      { case (Some(re1), Some(re2)) => HasPackage(toRegex(re1), toRegex(re2))
        case _                      => True
      }
    val comps = part.fold[FilterAST](True)(r => HasComponent(toRegex(r)))

    for {
      vpcs <- vinsWithPackagesAndComponents(namespace)
    } yield vpcs.filter(query(And(vins, And(pkgs, comps)))).map(_._1)

  }

  /*
   * Resolving package dependencies.
   */

  def resolve(namespace: Namespace, pkgId: PackageId)
             (implicit ec: ExecutionContext): DBIO[Map[Vehicle.Vin, Seq[PackageId]]] =
    for {
      _    <- PackageRepository.exists(namespace, pkgId)
      fs   <- PackageFilterRepository.listFiltersForPackage(namespace, pkgId)
      vpcs <- vinsWithPackagesAndComponents(namespace)
    } yield ResolveFunctions.makeFakeDependencyMap(pkgId,
              vpcs.filter(query(fs.map(_.expression).map(parseValidFilter).foldLeft[FilterAST](True)(And)))
                  .map(_._1))
}
