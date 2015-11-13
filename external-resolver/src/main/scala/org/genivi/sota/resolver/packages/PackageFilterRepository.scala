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
 * Data access object for PackageFilters
 */
object PackageFilterRepository {

  /**
   * DAO Mapping Class for the PackageFilters table in the database
   */
  // scalastyle:off
  class PackageFilterTable(tag: Tag) extends Table[PackageFilter](tag, "PackageFilter") {

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
