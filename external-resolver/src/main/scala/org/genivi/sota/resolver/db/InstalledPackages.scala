/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.vehicle.Vehicle
import org.genivi.sota.resolver.types.Package
import slick.driver.MySQLDriver.api._


object InstalledPackages {

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

  def add(vin: Vehicle.Vin, pkgId: Package.Id): DBIO[Int] =
    installedPackages.insertOrUpdate((vin, pkgId))

  def remove(vin: Vehicle.Vin, pkgId: Package.Id): DBIO[Int] =
    installedPackages.filter(ip =>
      ip.vin === vin &&
      ip.packageName === pkgId.name &&
      ip.packageVersion === pkgId.version).delete

  def list: DBIO[Seq[(Vehicle.Vin, Package.Id)]] =
    installedPackages.result

}
