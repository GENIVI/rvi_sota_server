/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import akka.http.scaladsl.model.Uri
import org.genivi.sota.core.data.Package
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.PackageId
import org.genivi.sota.db.Operators.regex
import org.genivi.sota.db.SlickExtensions._
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

/**
 * Database mapping definition for the Package SQL table. This defines all the
 * packages that have been uploaded to SOTA. Packages identified by a name +
 * version tuple. The database doesn't include the actual binary data, rather
 * A URL that points to it and a checksum is stored in the database instead.
 * There are also free-form descriptions about the package and vendor
 */
object Packages {

  import org.genivi.sota.refined.SlickRefined._

  /**
   * Slick mapping definition for the Package table
   * @see [[http://slick.typesafe.com/]]
   */
  // scalastyle:off
  class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

    def namespace = column[Namespace]("namespace")
    def name = column[PackageId.Name]("name")
    def version = column[PackageId.Version]("version")
    def uri = column[Uri]("uri")
    def size = column[Long]("file_size")
    def checkSum = column[String]("check_sum")
    def description = column[String]("description")
    def vendor = column[String]("vendor")
    def signature = column[String]("signature")

    def pk = primaryKey("pk_package", (namespace, name, version))

    def * = (namespace, name, version, uri, size, checkSum, description.?, vendor.?, signature.?).shaped <>
    (x => Package(x._1, PackageId(x._2, x._3), x._4, x._5, x._6, x._7, x._8, x._9),
    (x: Package) => Some((x.namespace, x.id.name, x.id.version, x.uri, x.size, x.checkSum, x.description, x.vendor, x.signature)))
  }
  // scalastyle:on

  /**
   * Internal helper definition to accesss the SQL table
   */
  val packages = TableQuery[PackageTable]

  /**
   * List all the packages that are available on the SOTA system
   * @return a list of packages
   */
  def list: DBIO[Seq[Package]] = packages.result

  /**
   * Add a new package to the SOTA system. If the package already exists, it is
   * updated in place.
   * @param pkg The definition of the package to add to SOTA
   * @return The package that was added
   */
  def create(pkg: Package)(implicit ec: ExecutionContext): DBIO[Package] =
    packages.insertOrUpdate(pkg).map(_ => pkg)

  /**
   * Find a package using a regular expression match on its name or version
   * @param reg The regular expression to search with
   */
  def searchByRegex(ns: Namespace, reg:String): DBIO[Seq[Package]] =
    packages.filter(p => p.namespace === ns && (regex(p.name, reg) || regex(p.version, reg))).result

  /**
   * Return the information about a package from its name & version
   * @param id The name/version of the package to fetch
   * @return The full package information
   */
  def byId(ns: Namespace, id : PackageId) : DBIO[Option[Package]] =
    packages.filter(p => p.namespace === ns && p.name === id.name && p.version === id.version).result.headOption

  /**
    * Return information about a list of packages. The complete package
    * information for every item in ids is returned
    * @param ids A set of package names/values to look up
    * @return A list of package definitions
    */
  def byIds(ns: Namespace, ids : Set[PackageId] )
           (implicit ec: ExecutionContext): DBIO[Seq[Package]] = {

    packages.filter(x =>
      x.namespace === ns &&
      (x.name.mappedTo[String] ++ x.version.mappedTo[String] inSet ids.map( id => id.name.get + id.version.get))
    ).result
  }
}
