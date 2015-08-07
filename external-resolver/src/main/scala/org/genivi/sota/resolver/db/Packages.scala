/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.resolver.types.{Package, PackageId}
import scala.concurrent.ExecutionContext

object Packages {

  import slick.driver.MySQLDriver.api._

  implicit val packageIdColumnType = MappedColumnType.base[PackageId, Long](
    { _.id },
    { PackageId.apply }
  )

  // scalastyle:off
  class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

    def id          = column[PackageId]("id", O.PrimaryKey, O.AutoInc)
    def name        = column[String]("name")
    def version     = column[String]("version")
    def description = column[String]("description")
    def vendor      = column[String]("vendor")

    def * = (id.?, name, version, description.?, vendor.?) <>
      ((Package.apply _).tupled, Package.unapply)
  }
  // scalastyle:on

  val packages = TableQuery[PackageTable]

  def add(pkg : Package.ValidPackage)(implicit ec: ExecutionContext): DBIO[Package] =
    ((packages returning packages.map(_.id)) += pkg)
      .map(id => pkg.copy(id = Some(id)))
}
