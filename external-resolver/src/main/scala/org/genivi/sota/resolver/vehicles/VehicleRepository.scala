/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.vehicles

import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.packages.Package
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

  def exists(vin: Vehicle.Vin): DBIO[Option[Vehicle]] =
    vehicles
      .filter(_.vin === vin)
      .result
      .headOption

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

  def installPackage(vin: Vehicle.Vin, pkgId: Package.Id): DBIO[Int] =
    installedPackages.insertOrUpdate((vin, pkgId))

  def uninstallPackage(vin: Vehicle.Vin, pkgId: Package.Id): DBIO[Int] =
    installedPackages.filter(ip =>
      ip.vin === vin &&
      ip.packageName === pkgId.name &&
      ip.packageVersion === pkgId.version).delete

  import org.genivi.sota.db.SlickExtensions._

  def updateInstalledPackages( vehicle: Vehicle, newPackages: Set[Package.Id], deletedPackages: Set[Package.Id] )
                             (implicit ec: ExecutionContext) : DBIO[Unit] = DBIO.seq(
    installedPackages.filter( ip =>
      ip.vin === vehicle.vin &&
      (ip.packageName.mappedTo[String] ++ ip.packageVersion.mappedTo[String])
        .inSet( deletedPackages.map( id => id.name.get + id.version.get ))
    ).delete,
    installedPackages ++= newPackages.map( vehicle.vin -> _ )
  ).transactionally

  def installedOn(vin: Vehicle.Vin)
                 (implicit ec: ExecutionContext) : DBIO[Set[Package.Id]] =
    installedPackages.filter(_.vin === vin).result.map( _.map( _._2).toSet )

  def listInstalledPackages: DBIO[Seq[(Vehicle.Vin, Package.Id)]] =
    installedPackages.result

}
