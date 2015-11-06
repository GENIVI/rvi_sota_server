/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.filters.{Filter, FilterRepository}
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import org.genivi.sota.db.SlickExtensions._

/**
 * Data access object for Packages
 */
object PackageRepository {

  /**
   * DAO Mapping Class for the Package table in the database
   */
  // scalastyle:off
  private[packages] class PackageTable(tag: Tag) extends Table[Package](tag, "Package") {

    def name        = column[Package.Name]("name")
    def version     = column[Package.Version]("version")
    def description = column[String]("description")
    def vendor      = column[String]("vendor")

    def pk = primaryKey("pk_package", (name, version))

    def * = (name, version, description.?, vendor.?).shaped <>
      (pkg => Package(Package.Id(pkg._1, pkg._2), pkg._3, pkg._4),
        (pkg: Package) => Some((pkg.id.name, pkg.id.version, pkg.description, pkg.vendor)))
  }
  // scalastyle:on

  val packages = TableQuery[PackageTable]

  /**
   * Adds a package to the Package table. Updates an existing package if already present.
   * @param pkg   The package to add
   * @return      A DBIO[Int] for the number of rows inserted
   */
  def add(pkg: Package): DBIO[Int] =
    packages.insertOrUpdate(pkg)

  /**
   * Lists the packages in the Package table
   * @return     A DBIO[Seq[Package]] for the packages in the table
   */
  def list: DBIO[Seq[Package]] =
    packages.result

  /**
   * Checks to see if a package exists in the database
   * @param pkgId   The Id of the package to check for
   * @return        The DBIO[Package] if the package exists
   * @throws        Errors.MissingPackageException if thepackage does not exist
   */
  def exists(pkgId: Package.Id)(implicit ec: ExecutionContext): DBIO[Package] =
    packages
      .filter(id => id.name    === pkgId.name &&
                    id.version === pkgId.version)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[Package]]
          (DBIO.failed(Errors.MissingPackageException))
          (DBIO.successful(_)))

  /**
   * Loads a group of Packages from the database by ID
   * @param ids     A Set[Package.Id] of Ids to load
   * @return        A DBIO[Set[Package]] of matched packages
   */
  def load(ids: Set[Package.Id])
          (implicit ec: ExecutionContext): DBIO[Set[Package]] = {
    packages.filter( x =>
      x.name.mappedTo[String] ++ x.version.mappedTo[String] inSet ids.map( id => id.name.get + id.version.get )
    ).result.map( _.toSet )
  }

}

/**
 * Data access object for PackageFilters
 */
object PackageFilterRepository {

  /**
   * DAO Mapping Class for the PackageFilters table in the database
   */
  // scalastyle:off
  class PackageFilterTable(tag: Tag) extends Table[PackageFilter](tag, "PackageFilters") {

    def packageName    = column[Package.Name]("packageName")
    def packageVersion = column[Package.Version]("packageVersion")
    def filterName     = column[Filter.Name]("filterName")

    def pk = primaryKey("pk_packageFilter", (packageName, packageVersion, filterName))

    def * = (packageName, packageVersion, filterName).shaped <>
      (p => PackageFilter(p._1, p._2, p._3),
        (pf: PackageFilter) => Some((pf.packageName, pf.packageVersion, pf.filterName)))
  }
  // scalastyle:on

  val packageFilters = TableQuery[PackageFilterTable]

  /**
   * Lists the package filters in the PackageFilter table
   * @return     A DBIO[Seq[PackageFilter]] for the package filters in the table
   */
  def list: DBIO[Seq[PackageFilter]] = packageFilters.result

  case object MissingPackageFilterException extends Throwable

  def exists(pf: PackageFilter)(implicit ec: ExecutionContext): DBIO[PackageFilter] =
    packageFilters
      .filter(r => r.packageName    === pf.packageName
                && r.packageVersion === pf.packageVersion
                && r.filterName     === pf.filterName)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[PackageFilter]]
          (DBIO.failed(MissingPackageFilterException))
          (DBIO.successful(_)))

  /**
   * Adds a package filter to the resolver
   * @param pf   The filter to add
   * @return     A DBIO[PackageFilter] for the added PackageFilter
   * @throws     Errors.MissingPackageException if the named package does not exist
   * @throws     Errors.MissingFilterException if the named filter does not exist
   */
  def addPackageFilter(pf: PackageFilter)
                      (implicit db: Database, ec: ExecutionContext): DBIO[PackageFilter] =
      for {
        _ <- PackageRepository.exists(Package.Id(pf.packageName, pf.packageVersion))
        _ <- FilterRepository.exists(pf.filterName)
        _ <- packageFilters += pf
      } yield pf


  /**
   * Lists the packages for a filter
   * @param fname  The name of the filter for which to list the packages
   * @return       A DBIO[Seq[Package]] of associated packages
   * @throws       Errors.MissingFilterException if the named filter does not exist
   */
  def listPackagesForFilter(fname: Filter.Name)
                           (implicit db: Database, ec: ExecutionContext): DBIO[Seq[Package]] =

    FilterRepository.exists(fname) andThen {
      val q = for {
        pf <- packageFilters.filter(_.filterName === fname)
        ps <- PackageRepository.packages
                .filter(pkg => pkg.name    === pf.packageName
                            && pkg.version === pf.packageVersion)
      } yield ps
      q.result
    }

  /**
   * Lists the filters for a specific package
   * @param pkgId  The ID of the package for which to find matching filters
   * @return       A DBIO[(Option[Package], Seq[Filter)] of the matching Package if found
   *               and a Seq of associated Filters, or None and the empty Seq
   */
  def listFiltersForPackage(pkgId: Package.Id)
                           (implicit ec: ExecutionContext): DBIO[(Option[Package], Seq[Filter])] = {
    val qFilters = for {
      pf <- packageFilters if pf.packageName    === pkgId.name &&
                              pf.packageVersion === pkgId.version
      f  <- FilterRepository.filters if f.name === pf.filterName
    } yield f

    for {
      p  <- PackageRepository.packages
              .filter(pkg => pkg.name    === pkgId.name
                          && pkg.version === pkgId.version)
              .result
              .headOption
      fs <- qFilters.result
    } yield (p, fs)
  }

  /**
   * Deletes a filter by name
   * @param fname   The name of the filter to delete
   * @return        A DBIO[Int] number of filters deleted
   */
  def deletePackageFilterByFilterName(fname: Filter.Name): DBIO[Int] =
    packageFilters.filter(_.filterName === fname).delete

  /**
   * Deletes a package filter
   * @param pf    The name of the package filter to be deleted
   * @return      A DBIO[Int] number of filters deleted
   */
  def deletePackageFilter(pf: PackageFilter)(implicit ec: ExecutionContext): DBIO[Int] =
    PackageFilterRepository.exists(pf) andThen
    packageFilters
      .filter(r => r.packageName    === pf.packageName
                && r.packageVersion === pf.packageVersion
                && r.filterName     === pf.filterName)
      .delete

}
