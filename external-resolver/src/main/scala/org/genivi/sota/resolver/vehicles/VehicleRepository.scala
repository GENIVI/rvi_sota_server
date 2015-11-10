/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.vehicles

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.components.{Component, ComponentRepository}
import org.genivi.sota.resolver.filters.FilterAST, FilterAST.{query, parseFilter, parseValidFilter}
import org.genivi.sota.resolver.filters.{True, And, VinMatches, HasPackage, HasComponent}
import org.genivi.sota.resolver.packages.{Package, PackageRepository, PackageFilterRepository}
import org.genivi.sota.resolver.resolve.ResolveFunctions
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


object VehicleRepository {

  // scalastyle:off
  class VinTable(tag: Tag) extends Table[Vehicle](tag, "Vehicle") {
    def vin = column[Vehicle.Vin]("vin")
    def * = (vin) <> (Vehicle.apply, Vehicle.unapply)
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
        fold[DBIO[Vehicle]](DBIO.failed(Errors.MissingVehicle))(DBIO.successful(_)))

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
   * Installed packages.
   */

  // scalastyle:off
  class InstalledPackageTable(tag: Tag) extends Table[(Vehicle.Vin, Package.Id)](tag, "InstalledPackage") {

    def vin            = column[Vehicle.Vin]    ("vin")
    def packageName    = column[Package.Name]   ("packageName")
    def packageVersion = column[Package.Version]("packageVersion")

    def pk = primaryKey("pk_installedPackage", (vin, packageName, packageVersion))

    def * = (vin, packageName, packageVersion).shaped <>
      (p => (p._1, Package.Id(p._2, p._3)),
      (vp: (Vehicle.Vin, Package.Id)) => Some((vp._1, vp._2.name, vp._2.version)))
  }
  // scalastyle:on

  val installedPackages = TableQuery[InstalledPackageTable]

  def installPackage
    (vin: Vehicle.Vin, pkgId: Package.Id)
    (implicit ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- exists(vin)
      _ <- PackageRepository.exists(pkgId)
      _ <- installedPackages.insertOrUpdate((vin, pkgId))
    } yield ()

  def uninstallPackage
    (vin: Vehicle.Vin, pkgId: Package.Id)
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

  def updateInstalledPackages(vin: Vehicle.Vin, packages: Set[Package.Id] )
                             (implicit ec: ExecutionContext): DBIO[Unit] = {

    def checkPackagesAvailable( ids: Set[Package.Id] ) : DBIO[Unit] = {
      PackageRepository.load(ids).map( ids -- _.map(_.id) ).flatMap { xs =>
        if( xs.isEmpty ) DBIO.successful(()) else DBIO.failed( Errors.MissingPackageException )
      }
    }

    def helper( vehicle: Vehicle, newPackages: Set[Package.Id], deletedPackages: Set[Package.Id] )
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
                 (implicit ec: ExecutionContext) : DBIO[Set[Package.Id]] =
    installedPackages.filter(_.vin === vin).result.map( _.map( _._2).toSet )

  def listInstalledPackages: DBIO[Seq[(Vehicle.Vin, Package.Id)]] =
    installedPackages.result

  def deleteInstalledPackageByVin(vin: Vehicle.Vin): DBIO[Int] =
    installedPackages.filter(_.vin === vin).delete

  def packagesOnVinMap
    (implicit ec: ExecutionContext)
      : DBIO[Map[Vehicle.Vin, Seq[Package.Id]]] =
    listInstalledPackages
      .map(_
        .sortBy(_._1)
        .groupBy(_._1)
        .mapValues(_.map(_._2)))

  def packagesOnVin
    (vin: Vehicle.Vin)
    (implicit ec: ExecutionContext): DBIO[Seq[Package.Id]] =
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
      : DBIO[Seq[(Vehicle, (Seq[Package.Id], Seq[Component.PartNumber]))]] =
    for {
      vs   <- VehicleRepository.list
      ps   : Seq[Seq[Package.Id]]
           <- DBIO.sequence(vs.map(v => VehicleRepository.packagesOnVin(v.vin)))
      cs   : Seq[Seq[Component.PartNumber]]
           <- DBIO.sequence(vs.map(v => VehicleRepository.componentsOnVin(v.vin)))
      vpcs : Seq[(Vehicle, (Seq[Package.Id], Seq[Component.PartNumber]))]
           =  vs.zip(ps.zip(cs))
    } yield vpcs

  /*
   * Searching
   */

  def search(re        : Option[Refined[String, Regex]],
             pkgName   : Option[Package.Name],
             pkgVersion: Option[Package.Version],
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

  def resolve(pkgId: Package.Id)
             (implicit ec: ExecutionContext): DBIO[Map[Vehicle.Vin, Seq[Package.Id]]] =
    for {
      _       <- PackageRepository.exists(pkgId)
      (_, fs) <- PackageFilterRepository.listFiltersForPackage(pkgId)
      vpcs    <- vinsWithPackagesAndComponents
    } yield ResolveFunctions.makeFakeDependencyMap(pkgId,
              vpcs.filter(query(fs.map(_.expression).map(parseValidFilter).foldLeft[FilterAST](True)(And)))
                  .map(_._1))
}
