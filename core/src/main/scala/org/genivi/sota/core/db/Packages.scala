/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.model.Uri
import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.core.data.Package
import org.genivi.sota.data.Namespace
import org.genivi.sota.data.PackageId
import slick.jdbc.MySQLProfile.api._

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
  import org.genivi.sota.db.Operators._
  import org.genivi.sota.db.SlickExtensions._

  /**
   * Slick mapping definition for the Package table
   * @see [[http://slick.typesafe.com/]]
   */
  // scalastyle:off
  class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

    def uuid = column[UUID]("uuid")
    def namespace = column[Namespace]("namespace")
    def name = column[PackageId.Name]("name")
    def version = column[PackageId.Version]("version")
    def uri = column[Uri]("uri")
    def size = column[Long]("file_size")
    def checkSum = column[String]("check_sum")
    def description = column[String]("description")
    def vendor = column[String]("vendor")
    def signature = column[String]("signature")
    def createdAt = column[Instant]("created_at")

    def pk = primaryKey("pk_package", uuid)

    def uniqueUuid = index("Package_unique_name_version", (namespace, name, version), unique = true)

    def * = (namespace, name, version, uri, size, checkSum, description.?, vendor.?, signature.?, uuid, createdAt).shaped <>
      (x => Package(x._1, x._10, PackageId(x._2, x._3), x._4, x._5, x._6, x._7, x._8, x._9, x._11),
        (x: Package) => Some((x.namespace, x.id.name, x.id.version, x.uri, x.size, x.checkSum, x.description, x.vendor,
                              x.signature, x.uuid, x.createdAt)))
  }
  // scalastyle:on

  /**
   * Internal helper definition to access the SQL table
   */
  val packages = TableQuery[PackageTable]

  /**
   * List all the packages that are available on the SOTA system
   * @return a list of packages
   */
  def list(ns: Namespace): DBIO[Seq[Package]] = packages.filter(_.namespace === ns).result

  /**
   * Add a new package to the SOTA system. If the package already exists, it is
   * updated in place.
   * @param pkg The definition of the package to add to SOTA
   * @return The package that was added
   */
  def create(pkg: Package)(implicit ec: ExecutionContext): DBIO[Package] = {
    def findById(packageId: PackageId): DBIO[Option[Package]] =
      packages
        .filter(_.namespace === pkg.namespace).filter(_.name === pkg.id.name).filter(_.version === pkg.id.version)
        .result.headOption

    val dbio = for {
      maybeExisting <- findById(pkg.id)
      newPkg = maybeExisting match {
        case Some(existing) => pkg.copy(uuid = existing.uuid)
        case None => pkg
      }
      _ <- packages.insertOrUpdate(newPkg)
    } yield pkg

    dbio.transactionally
  }

  def searchByRegexWithBlacklist(ns: Namespace, reg: Option[String]): DBIO[Seq[(Package, Boolean)]] =
    packages
      .filter(_.namespace === ns)
      .regexFilter(reg)(_.name, _.version)
      .joinLeft(BlacklistedPackages.active)
      .on { case (p, bl) => p.namespace === bl.namespace && p.name === bl.pkgName && p.version === bl.pkgVersion }
      .map { case (pkg, blacklistO) =>  (pkg, blacklistO.map(_.active).getOrElse(false)) }
      .result

  def byId(ns: Namespace, id : PackageId)(implicit ec: ExecutionContext): DBIO[Package] =
    packages
      .filter(p => p.namespace === ns && p.name === id.name && p.version === id.version)
      .result
      .failIfNotSingle(SotaCoreErrors.MissingPackage)

  /**
    * Fetch from DB the [[Package]]s corresponding to the given [[PackageId]]s.
    */
  def byIds(ns: Namespace, ids : Set[PackageId] )
           (implicit ec: ExecutionContext): DBIO[Seq[Package]] = {

    packages.filter(x =>
      x.namespace === ns &&
      (x.name.mappedTo[String] ++ x.version.mappedTo[String] inSet ids.map( id => id.name.get + id.version.get))
    ).result
  }

  def byUuid(uuid: UUID)(implicit ec: ExecutionContext): DBIO[Package] =
    packages.filter(_.uuid === uuid).result.failIfNotSingle(SotaCoreErrors.MissingPackage)

  /**
   * Update the description about a package from its name & version
   * @param id The name/version of the package to update
   * @param description the new description
   */
  def updateInfo(ns: Namespace, id : PackageId, description: String)
                (implicit ec: ExecutionContext): DBIO[Unit] = {
    packages.filter(p => p.namespace === ns && p.name === id.name && p.version === id.version)
            .map(_.description).update(description).map(_ => ())
  }

  def find(ns: Namespace, packageId: PackageId)
          (implicit ec: ExecutionContext): DBIO[Package] = {
    packages
      .filter(_.namespace === ns)
      .filter(_.name === packageId.name)
      .filter(_.version === packageId.version)
      .result
      .failIfNotSingle(SotaCoreErrors.MissingPackage)
  }

  def usage(ns: Namespace)(implicit ec: ExecutionContext): DBIO[Long] =
    packages
      .filter(_.namespace === ns)
      .map(_.size)
      .sum
      .result
      .map(_.getOrElse(0L))

  // find package corresponding to uuid and return namespaces it is stored under
  def packageNamespaces(packageUuid: UUID): DBIO[Seq[Namespace]] = {
    val query = for {
      byUuid <- packages if byUuid.uuid === packageUuid
      byNameAndVersion <- packages if byNameAndVersion.name === byUuid.name &&
                                      byNameAndVersion.version === byUuid.version
    } yield byNameAndVersion.namespace

    query.result
  }
}
