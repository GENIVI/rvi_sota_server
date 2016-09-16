/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.data.{Namespace, PackageId}
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.filters.{Filter, FilterRepository}
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext


/**
 * Data access object for PackageFilters
 */
object PackageFilterRepository {

  /**
   * DAO Mapping Class for the PackageFilters table in the database
   */
  // scalastyle:off
  class PackageFilterTable(tag: Tag) extends Table[PackageFilter](tag, "PackageFilter") {

    def namespace      = column[Namespace]("namespace")
    def packageName    = column[PackageId.Name]("packageName")
    def packageVersion = column[PackageId.Version]("packageVersion")
    def filterName     = column[Filter.Name]("filterName")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("pk_packageFilter", (namespace, packageName, packageVersion, filterName))

    def * = (namespace, packageName, packageVersion, filterName).shaped <>
      (p => PackageFilter(p._1, p._2, p._3, p._4),
        (pf: PackageFilter) => Some((pf.namespace, pf.packageName, pf.packageVersion, pf.filterName)))
  }
  // scalastyle:on

  val packageFilters = TableQuery[PackageFilterTable]

  /**
   * Lists the package filters in the PackageFilter table
    *
    * @return     A DBIO[Seq[PackageFilter]] for the package filters in the table
   */
  def list: DBIO[Seq[PackageFilter]] = packageFilters.result

  def exists(pf: PackageFilter)(implicit ec: ExecutionContext): DBIO[PackageFilter] =
    packageFilters
      .filter(r => r.namespace      === pf.namespace
                && r.packageName    === pf.packageName
                && r.packageVersion === pf.packageVersion
                && r.filterName     === pf.filterName)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[PackageFilter]]
          (DBIO.failed(Errors.MissingPackageFilter))
          (DBIO.successful))

  /**
   * Adds a package filter to the resolver
    *
    * @param pf   The filter to add
   * @return     A DBIO[PackageFilter] for the added PackageFilter
   * @throws     Errors.MissingPackageException if the named package does not exist
   * @throws     Errors.MissingFilterException if the named filter does not exist
   */
  def addPackageFilter(pf: PackageFilter)
                      (implicit db: Database, ec: ExecutionContext): DBIO[PackageFilter] =
    for {
      _ <- PackageRepository.exists(pf.namespace, PackageId(pf.packageName, pf.packageVersion))
      _ <- FilterRepository.exists(pf.namespace, pf.filterName)
      _ <- packageFilters += pf
    } yield pf

  /**
   * Lists the packages for a filter
    *
    * @param fname  The name of the filter for which to list the packages
   * @return       A DBIO[Seq[Package]] of associated packages
   * @throws       Errors.MissingFilterException if the named filter does not exist
   */
  def listPackagesForFilter(namespace: Namespace, fname: Filter.Name)
                           (implicit db: Database, ec: ExecutionContext): DBIO[Seq[Package]] =

    FilterRepository.exists(namespace, fname) andThen {
      val q = for {
        pf <- packageFilters.filter(i => i.namespace === namespace && i.filterName === fname)
        ps <- PackageRepository.packages
                .filter(pkg => pkg.name    === pf.packageName
                            && pkg.version === pf.packageVersion)
      } yield ps
      q.result
    }

  def listFiltersForPackage(namespace: Namespace, pkgId: PackageId)
                           (implicit ec: ExecutionContext): DBIO[Seq[Filter]] = {
    PackageRepository.exists(namespace, pkgId) andThen {
      val q = for {
        pf <- packageFilters.filter(pf => pf.namespace      === namespace
                                       && pf.packageName    === pkgId.name
                                       && pf.packageVersion === pkgId.version)
        fs <- FilterRepository.filters
          .filter(i => i.namespace === namespace && i.name === pf.filterName)
      } yield fs
      q.result
    }
  }


  /**
   * Deletes a filter by name
    *
    * @param fname   The name of the filter to delete
   * @return        A DBIO[Int] number of filters deleted
   */
  def deletePackageFilterByFilterName(namespace: Namespace, fname: Filter.Name): DBIO[Int] =
    packageFilters.filter(i => i.namespace === namespace && i.filterName === fname).delete

  /**
   * Deletes a package filter
    *
    * @param pf    The name of the package filter to be deleted
   * @return      A DBIO[Int] number of filters deleted
   */
  def deletePackageFilter(pf: PackageFilter)(implicit ec: ExecutionContext): DBIO[Int] =
    PackageFilterRepository.exists(pf) andThen
    packageFilters
      .filter(r => r.namespace      === pf.namespace
                && r.packageName    === pf.packageName
                && r.packageVersion === pf.packageVersion
                && r.filterName     === pf.filterName)
      .delete

}
