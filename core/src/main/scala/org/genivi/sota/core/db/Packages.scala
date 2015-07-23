package org.genivi.sota.core.db

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.Package

object Packages extends DatabaseConfig {

  class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def version = column[String]("version")
    def description = column[String]("description")
    def vendor = column[String]("description")

    def * = (id.?, name, version, description.?, vendor.?) <>
      ((Package.apply _).tupled, Package.unapply)
  }

  val packages = TableQuery[PackageTable]

  def list: Future[Seq[Package]] = db.run(packages.result)

  def create(pkg: Package)(implicit ec: ExecutionContext): Future[Package] =
    create(List(pkg)).map(_.head)

  def create(reqs: Seq[Package]): Future[Seq[Package]] = {
    val insertions = reqs.map { pkg =>
      (packages
         returning packages.map(_.id)
         into ((pkg, id) => pkg.copy(id = Some(id)))) += pkg
    }

    db.run(DBIO.sequence(insertions))
  }
}
