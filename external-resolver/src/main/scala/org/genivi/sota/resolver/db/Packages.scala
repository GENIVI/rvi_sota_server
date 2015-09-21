/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.types.Package
import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace
import slick.driver.MySQLDriver.api._


object Packages {

  // scalastyle:off
  class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

    def name        = column[Package.Name]("name")
    def version     = column[Package.Version]("version")
    def description = column[String]("description")
    def vendor      = column[String]("vendor")

    def pk = primaryKey("pk_package", (name, version))

    def * = (name, version, description.?, vendor.?).shaped <>
      (pkg => Package(Package.Id(pkg._1, pkg._2), pkg._3, pkg._4),
        (pkg: Package) => Some((pkg.id.name, pkg.id.version, pkg.description, pkg.vendor)))
  }
  // scalastyle:on

  val packages = TableQuery[PackageTable]

  def add(pkg: Package)(implicit ec: ExecutionContext): DBIO[Package] =
    packages.insertOrUpdate(pkg).map(_ => pkg)

  case object MissingPackageException extends Throwable with NoStackTrace

  def exists(name: Package.Name, version: Package.Version)(implicit ec: ExecutionContext): DBIO[Package] =
    packages
      .filter(p => p.name === name && p.version === version)
      .result
      .map(ps => if(ps.isEmpty) throw MissingPackageException else ps.head)
}
