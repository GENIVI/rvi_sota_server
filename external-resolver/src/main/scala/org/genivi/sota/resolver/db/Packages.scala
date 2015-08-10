/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.resolver.types.{Package, PackageId}
import scala.concurrent.ExecutionContext

object Packages {

  import org.genivi.sota.refined.SlickRefined._
  import slick.driver.MySQLDriver.api._

  implicit val packageIdColumnType = MappedColumnType.base[PackageId, Long](
    { _.id },
    { PackageId.apply }
  )

  // scalastyle:off
  class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

    def id          = column[PackageId]("id", O.AutoInc)
    def name        = column[Package.PackageName]("name")
    def version     = column[Package.Version]("version")
    def description = column[String]("description")
    def vendor      = column[String]("vendor")

    def pk = primaryKey("id", id)

    def * = (id.?, name, version, description.?, vendor.?) <>
      ((Package.apply _).tupled, Package.unapply)
  }
  // scalastyle:on

  val packages = TableQuery[PackageTable]

  def add(pkg : Package)(implicit ec: ExecutionContext): DBIO[Package] =
    ((packages returning packages.map(_.id)).insertOrUpdate(pkg))
      .map(id => pkg.copy(id = id))
}
