/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import org.genivi.sota.data.PackageId
import org.genivi.sota.db.Operators._
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.filters.{Filter, FilterRepository}
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


/**
 * Data access object for Packages
 */
object PackageRepository {

  /**
   * DAO Mapping Class for the Package table in the database
   */
  // scalastyle:off
  private[packages] class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

    def name        = column[PackageId.Name]("name")
    def version     = column[PackageId.Version]("version")
    def description = column[String]("description")
    def vendor      = column[String]("vendor")

    def pk = primaryKey("pk_package", (name, version))

    def * = (name, version, description.?, vendor.?).shaped <>
      (pkg => Package(PackageId(pkg._1, pkg._2), pkg._3, pkg._4),
        (pkg: Package) => Some((pkg.id.name, pkg.id.version, pkg.description, pkg.vendor)))
  }
  // scalastyle:on

  val packages = TableQuery[PackageTable]

  /**
   * Adds a package to the Package table. Updates an existing package if already present.
 *
   * @param pkg   The package to add
   * @return      A DBIO[Int] for the number of rows inserted
   */
  def add(pkg: Package): DBIO[Int] =
    packages.insertOrUpdate(pkg)

  /**
   * Lists the packages in the Package table
 *
   * @return     A DBIO[Seq[Package]] for the packages in the table
   */
  def list: DBIO[Seq[Package]] =
    packages.result

  /**
   * Checks to see if a package exists in the database
 *
   * @param pkgId   The Id of the package to check for
   * @return        The DBIO[Package] if the package exists
   * @throws        Errors.MissingPackageException if thepackage does not exist
   */
  def exists(pkgId: PackageId)(implicit ec: ExecutionContext): DBIO[Package] =
    packages
      .filter(id => id.name    === pkgId.name &&
                    id.version === pkgId.version)
      .result
      .headOption
      .failIfNone(Errors.MissingPackageException)

  /**
   * Loads a group of Packages from the database by ID
 *
   * @param ids     A Set[Package.Id] of Ids to load
   * @return        A DBIO[Set[Package]] of matched packages
   */
  def load(ids: Set[PackageId])
          (implicit ec: ExecutionContext): DBIO[Set[Package]] = {
    packages.filter( x =>
      x.name.mappedTo[String] ++ x.version.mappedTo[String] inSet ids.map( id => id.name.get + id.version.get )
    ).result.map( _.toSet )
  }

}
