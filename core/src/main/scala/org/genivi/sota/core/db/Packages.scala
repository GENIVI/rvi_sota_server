/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.genivi.sota.core.data.{Package, PackageId}
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._

object Packages {

  import org.genivi.sota.refined.SlickRefined._

  // scalastyle:off
  class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {
    def name = column[Package.Name]("name")
    def version = column[Package.Version]("version")
    def description = column[String]("description")
    def vendor = column[String]("vendor")

    def pk = primaryKey("pk_package", (name, version))

    def * = (name, version, description.?, vendor.?).shaped <>
      (x => Package(PackageId(x._1, x._2), x._3, x._4), (x: Package) => Some((x.id.name, x.id.version, x.description, x.vendor)))
  }
  // scalastyle:on

  val packages = TableQuery[PackageTable]

  def list: DBIO[Seq[Package]] = packages.result

  def create(pkg: Package)(implicit ec: ExecutionContext): DBIO[Package] = (packages += pkg).map( _ => pkg)

}
