/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import org.genivi.sota.refined.SlickRefined._
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import org.genivi.sota.db.SlickExtensions._

object PackageRepository {

  // scalastyle:off
  private[packages] class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

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

  def add(pkg: Package): DBIO[Int] =
    packages.insertOrUpdate(pkg)

  def list: DBIO[Seq[Package]] =
    packages.result

  def exists(pkgId: Package.Id): DBIO[Option[Package]] =
    packages
      .filter(id => id.name    === pkgId.name &&
                    id.version === pkgId.version)
      .result
      .headOption

  def load(ids: Set[Package.Id])
          (implicit ec: ExecutionContext): DBIO[Set[Package]] = {
    packages.filter( x =>
      x.name.mappedTo[String] ++ x.version.mappedTo[String] inSet ids.map( id => id.name.get + id.version.get )
    ).result.map( _.toSet )
  }
}
