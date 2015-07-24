package org.genivi.sota.core.db

import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.Package

object Packages {

  class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def version = column[String]("version")
    def description = column[String]("description")
    def vendor = column[String]("vendor")

    def * = (id.?, name, version, description.?, vendor.?) <>
      ((Package.apply _).tupled, Package.unapply)
  }

  val packages = TableQuery[PackageTable]

  def list = packages.result

  def create(pkg: Package) =
    (packages
      returning packages.map(_.id)
      into ((pkg, id) => pkg.copy(id = Some(id)))) += pkg

  def createPackages(reqs: Seq[Package]) = {
    DBIO.sequence( reqs.map( create ) )
  }
}
