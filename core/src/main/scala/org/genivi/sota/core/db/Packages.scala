/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import akka.http.scaladsl.model.Uri
import eu.timepit.refined.Refined
import org.genivi.sota.core.data.Package
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.generic.DeepHLister
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import org.genivi.sota.generic.DeepUnapply
import org.genivi.sota.db.Operators.regex
import slick.lifted.{LiteralColumn, ExtensionMethods, Rep, StringColumnExtensionMethods}

/**
 * Database mapping definition for the Package SQL table. This defines all the
 * packages that have been uploaded to SOTA. Packages identified by a name +
 * version tuple. The database doesn't include the actual binary data, rather
 * A URL that points to it and a checksum is stored in the database instead.
 * There are also free-form descriptions about the package and vendor
 */
object Packages {

  import org.genivi.sota.refined.SlickRefined._
  import shapeless._

  /**
   * Slick mapping definition for the Package table
   * @see {@linktourl http://slick.typesafe.com/}
   */
  // scalastyle:off
  class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {
    implicit def uriDeepHLister[T <: HList](implicit dhl: Lazy[DeepHLister[T]]) : DeepHLister.Aux[Uri :: T, Uri :: dhl.value.Out] = DeepHLister.headNotCaseClassDeepHLister

    def name = column[Package.Name]("name")
    def version = column[Package.Version]("version")
    def uri = column[Uri]("uri")
    def size = column[Long]("file_size")
    def checkSum = column[String]("check_sum")
    def description = column[String]("description")
    def vendor = column[String]("vendor")

    def pk = primaryKey("pk_package", (name, version))

    def * = (name, version, uri, size, checkSum, description.?, vendor.?).shaped <>
    (x => Package(Package.Id(x._1, x._2), x._3, x._4, x._5, x._6, x._7),
    (x: Package) => DeepUnapply(x))
  }
  // scalastyle:on

  /**
   * Internal helper definition to accesss the SQL table
   */
  val packages = TableQuery[PackageTable]

  /**
   * List all the packages that are available on the SOTA system
   */
  def list: DBIO[Seq[Package]] = packages.result

  /**
   * Add a new package to the SOTA system. If the package already exists, it is
   * updated in place.
   */
  def create(pkg: Package)(implicit ec: ExecutionContext): DBIO[Package] =
    packages.insertOrUpdate(pkg).map(_ => pkg)

  /**
   * Find a package using a regular expression match on its name or version
   */
  def searchByRegex(reg:String): DBIO[Seq[Package]] =
    packages.filter (packages => regex(packages.name, reg) || regex(packages.version, reg) ).result

  /**
   * Return the information about a package from its name & version
   */
  def byId(id : Package.Id) : DBIO[Option[Package]] =
    packages.filter( p => p.name === id.name && p.version === id.version ).result.headOption

  /**
    * Return information about a list of packages. The complete package
    * information for every item in ids is returned
    */
  def byIds(ids : Set[Package.Id] )
           (implicit ec: ExecutionContext): DBIO[Seq[Package]] = {

    packages.filter( x =>
      x.name.mappedTo[String] ++ x.version.mappedTo[String] inSet ids.map( id => id.name.get + id.version.get )
    ).result
  }
}
