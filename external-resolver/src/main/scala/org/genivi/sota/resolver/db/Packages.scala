package org.genivi.sota.core.db

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._
import org.genivi.sota.resolver.db.DatabaseConfig
import org.genivi.sota.resolver.Package

object Packages extends DatabaseConfig {

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

  def list: Future[Seq[Package]] = db.run(packages.result)

  def create(newPackage: Package)(implicit ec: ExecutionContext): Future[Package] =
    db.run(packages += newPackage).map(_ => newPackage)
}
