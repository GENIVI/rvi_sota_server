/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.vehicles

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
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
    def vin = column[Vehicle.Vin]("vin")
    def * = vin <> (Vehicle.apply, Vehicle.unapply)
    def pk = primaryKey("vin", vin)  // insertOrUpdate doesn't work if
                                     // we use O.PrimaryKey in the vin
                                     // column, see Slick issue #966.
  }
  // scalastyle:on

  val vehicles = TableQuery[VinTable]

  def add(vehicle: Vehicle): DBIO[Int] =
    vehicles.insertOrUpdate(vehicle)

  def list: DBIO[Seq[Vehicle]] =
    vehicles.result

  def exists(vin: Vehicle.Vin)(implicit ec: ExecutionContext): DBIO[Vehicle] =
    vehicles
      .filter(_.vin === vin)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[Vehicle]](DBIO.failed(Errors.MissingVehicle))(DBIO.successful))

  def delete(vin: Vehicle.Vin): DBIO[Int] =
    vehicles.filter(_.vin === vin).delete

  def deleteVin(vin: Vehicle.Vin)
               (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- VehicleRepository.exists(vin)
      _ <- deleteInstalledPackageByVin(vin)
      _ <- deleteInstalledComponentByVin(vin)
      _ <- delete(vin)
    } yield ()

  /*
   * Installed firmware.
   */

  // scalastyle:off
  class InstalledFirmwareTable(tag: Tag) extends Table[(Firmware, Vehicle.Vin)](tag, "Firmware") {

    def module        = column[Firmware.Module]     ("module")
    def firmware_id   = column[Firmware.FirmwareId] ("firmware_id")
    def last_modified = column[DateTime]            ("last_modified")
    def vin           = column[Vehicle.Vin]         ("vin")

    def pk = primaryKey("pk_installedFirmware", (module, firmware_id, vin))

    def * = (module, firmware_id, last_modified, vin).shaped <>
      (p => (Firmware(p._1, p._2, p._3), p._4),
        (fw: (Firmware, Vehicle.Vin)) => Some((fw._1.module, fw._1.firmwareId, fw._1.lastModified, fw._2)))
  }
  // scalastyle:on

  val installedFirmware = TableQuery[InstalledFirmwareTable]

  def firmwareExists(module: Firmware.Module)(implicit ec: ExecutionContext): DBIO[Firmware.Module] = {
    val res = for {
      ifw <- installedFirmware.filter(_.module === module).result.headOption
    } yield ifw
    res.flatMap(_.fold[DBIO[Firmware.Module]]
      (DBIO.failed(Errors.MissingFirmwareException))(x => DBIO.successful(x._1.module)))
  }

  def installFirmware
    (module: Firmware.Module, firmware_id: Firmware.FirmwareId, last_modified: DateTime, vin: Vehicle.Vin)
    (implicit ec: ExecutionContext): DBIO[Unit] = {
      for {
        _ <- exists(vin)
        _ <- firmwareExists(module)
        _ <- installedFirmware.insertOrUpdate((Firmware(module, firmware_id, last_modified), vin))
      } yield()
  }

  def firmwareOnVin
    (vin: Vehicle.Vin)
    (implicit ec: ExecutionContext): DBIO[Seq[Firmware]] = {
      for {
        _  <- VehicleRepository.exists(vin)
        ps <- installedFirmware.filter(_.vin === vin).result
      } yield ps.map(_._1)
  }

  /*
   * Installed packages.
   */

  // scalastyle:off
  class InstalledPackageTable(tag: Tag) extends Table[(Vehicle.Vin, PackageId)](tag, "InstalledPackage") {

    def vin            = column[Vehicle.Vin]    ("vin")
    def packageName    = column[PackageId.Name]   ("packageName")
    def packageVersion = column[PackageId.Version]("packageVersion")

    def pk = primaryKey("pk_installedPackage", (vin, packageName, packageVersion))

    def * = (vin, packageName, packageVersion).shaped <>
      (p => (p._1, PackageId(p._2, p._3)),
      (vp: (Vehicle.Vin, PackageId)) => Some((vp._1, vp._2.name, vp._2.version)))
  }
  // scalastyle:on

  val installedPackages = TableQuery[InstalledPackageTable]

  def installPackage
    (vin: Vehicle.Vin, pkgId: PackageId)
    (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- exists(vin)
      _ <- PackageRepository.exists(pkgId)
      _ <- installedPackages.insertOrUpdate((vin, pkgId))
    } yield ()

  def uninstallPackage
    (vin: Vehicle.Vin, pkgId: PackageId)
    (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- exists(vin)
      _ <- PackageRepository.exists(pkgId)
      _ <- installedPackages.filter { ip =>
             ip.vin            === vin &&
             ip.packageName    === pkgId.name &&
             ip.packageVersion === pkgId.version
           }.delete
    } yield ()

  def updateInstalledPackages(vin: Vehicle.Vin, packages: Set[PackageId] )
                             (implicit ec: ExecutionContext): DBIO[Unit] = {

    def checkPackagesAvailable( ids: Set[PackageId] ) : DBIO[Unit] = {
      PackageRepository.load(ids).map( ids -- _.map(_.id) ).flatMap { xs =>
        if( xs.isEmpty ) DBIO.successful(()) else DBIO.failed( Errors.MissingPackageException )
      }
    }

    def helper( vehicle: Vehicle, newPackages: Set[PackageId], deletedPackages: Set[PackageId] )
                               (implicit ec: ExecutionContext) : DBIO[Unit] = DBIO.seq(
      installedPackages.filter( ip =>
        ip.vin === vehicle.vin &&
        (ip.packageName.mappedTo[String] ++ ip.packageVersion.mappedTo[String])
          .inSet( deletedPackages.map( id => id.name.get + id.version.get ))
      ).delete,
      installedPackages ++= newPackages.map( vehicle.vin -> _ )
    ).transactionally

    for {
      vehicle           <- VehicleRepository.exists(vin)
      installedPackages <- VehicleRepository.installedOn(vin)
      newPackages       =  packages -- installedPackages
      deletedPackages   =  installedPackages -- packages
      _                 <- checkPackagesAvailable(newPackages)
      _                 <- helper(vehicle, newPackages, deletedPackages)
    } yield ()
  }

  def installedOn(vin: Vehicle.Vin)
                 (implicit ec: ExecutionContext) : DBIO[Set[PackageId]] =
    installedPackages.filter(_.vin === vin).result.map( _.map( _._2).toSet )

  def listInstalledPackages: DBIO[Seq[(Vehicle.Vin, PackageId)]] =
    installedPackages.result

  def deleteInstalledPackageByVin(vin: Vehicle.Vin): DBIO[Int] =
    installedPackages.filter(_.vin === vin).delete

  def packagesOnVinMap
    (implicit ec: ExecutionContext)
      : DBIO[Map[Vehicle.Vin, Seq[PackageId]]] =
    listInstalledPackages
      .map(_
        .sortBy(_._1)
        .groupBy(_._1)
        .mapValues(_.map(_._2)))

  def packagesOnVin
    (vin: Vehicle.Vin)
    (implicit ec: ExecutionContext): DBIO[Seq[PackageId]] =
    for {
      _  <- VehicleRepository.exists(vin)
      ps <- packagesOnVinMap
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
      extends Table[(Vehicle.Vin, Component.PartNumber)](tag, "InstalledComponent") {

    def vin        = column[Vehicle.Vin]         ("vin")
    def partNumber = column[Component.PartNumber]("partNumber")

    def pk = primaryKey("pk_installedComponent", (vin, partNumber))

    def * = (vin, partNumber)
  }
  // scalastyle:on

  val installedComponents = TableQuery[InstalledComponentTable]

  def listInstalledComponents: DBIO[Seq[(Vehicle.Vin, Component.PartNumber)]] =
    installedComponents.result

  def deleteInstalledComponentByVin(vin: Vehicle.Vin): DBIO[Int] =
    installedComponents.filter(_.vin === vin).delete

  def installComponent(vin: Vehicle.Vin, part: Component.PartNumber)
                      (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- VehicleRepository.exists(vin)
      _ <- ComponentRepository.exists(part)
      _ <- installedComponents += ((vin, part))
    } yield ()

  def uninstallComponent(vin: Vehicle.Vin, part: Component.PartNumber): DBIO[Int] =
    ???

  def componentsOnVinMap
    (implicit ec: ExecutionContext): DBIO[Map[Vehicle.Vin, Seq[Component.PartNumber]]] =
    VehicleRepository.listInstalledComponents
      .map(_
        .sortBy(_._1)
        .groupBy(_._1)
        .mapValues(_.map(_._2)))

  def componentsOnVin(vin: Vehicle.Vin)
                     (implicit ec: ExecutionContext): DBIO[Seq[Component.PartNumber]] =
    for {
      _  <- exists(vin)
      cs <- componentsOnVinMap
              .map(_
                .get(vin)
                .toList
                .flatten)
    } yield cs

  def vinsWithPackagesAndComponents
    (implicit ec: ExecutionContext)
      : DBIO[Seq[(Vehicle, (Seq[PackageId], Seq[Component.PartNumber]))]] =
    for {
      vs   <- VehicleRepository.list
      ps   : Seq[Seq[PackageId]]
           <- DBIO.sequence(vs.map(v => VehicleRepository.packagesOnVin(v.vin)))
      cs   : Seq[Seq[Component.PartNumber]]
           <- DBIO.sequence(vs.map(v => VehicleRepository.componentsOnVin(v.vin)))
      vpcs : Seq[(Vehicle, (Seq[PackageId], Seq[Component.PartNumber]))]
           =  vs.zip(ps.zip(cs))
    } yield vpcs

  /*
   * Searching
   */

  def search(re        : Option[Refined[String, Regex]],
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
      vpcs <- vinsWithPackagesAndComponents
    } yield vpcs.filter(query(And(vins, And(pkgs, comps)))).map(_._1)

  }

  /*
   * Resolving package dependencies.
   */

  def resolve(pkgId: PackageId)
             (implicit ec: ExecutionContext): DBIO[Map[Vehicle.Vin, Seq[PackageId]]] =
    for {
      _    <- PackageRepository.exists(pkgId)
      fs   <- PackageFilterRepository.listFiltersForPackage(pkgId)
      vpcs <- vinsWithPackagesAndComponents
    } yield ResolveFunctions.makeFakeDependencyMap(pkgId,
              vpcs.filter(query(fs.map(_.expression).map(parseValidFilter).foldLeft[FilterAST](True)(And)))
                  .map(_._1))
}
