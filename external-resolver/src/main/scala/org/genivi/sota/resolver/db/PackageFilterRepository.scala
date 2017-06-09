/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import java.util.UUID

import org.genivi.sota.data.{Namespace, PackageId}
import org.genivi.sota.refined.PackageIdDatabaseConversions.LiftedPackageId
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.filters.{Filter, FilterRepository}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext


/**
 * Data access object for PackageFilters
 */
object PackageFilterRepository {

  import org.genivi.sota.db.SlickExtensions._

  // scalastyle:off
  class PackageFilterTable(tag: Tag) extends Table[PackageFilter](tag, "PackageFilter") {

    def namespace = column[Namespace]("namespace")
    def packageUuid = column[UUID]("package_uuid")
    def filterName = column[Filter.Name]("filterName")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("pk_packageFilter", (packageUuid, filterName))

    def pkg_filter_fk = foreignKey("filter_fk", (namespace, filterName),
      FilterRepository.filters)(r => (r.namespace, r.name))

    def * = (namespace, packageUuid, filterName).shaped <>
      (p => PackageFilter(p._1, p._2, p._3),
        (pf: PackageFilter) => Some((pf.namespace, pf.packageUuid, pf.filterName)))
  }
  // scalastyle:on

  val packageFilters = TableQuery[PackageFilterTable]

  /**
   * Lists the package filters in the PackageFilter table
    *
    * @return     A DBIO[Seq[PackageFilter]] for the package filters in the table
   */
  def list(namespace: Namespace): DBIO[Seq[(PackageFilter, PackageId)]] =
    packageFilters.filter(_.namespace === namespace)
      .join(PackageRepository.packages.filter(_.namespace === namespace))
      .on(_.packageUuid === _.uuid)
      .map { case (filter, pkg) => (filter, LiftedPackageId(pkg.name, pkg.version)) }
      .result

  def addPackageFilter(namespace: Namespace, packageId: PackageId, name: Filter.Name)
                      (implicit db: Database, ec: ExecutionContext): DBIO[PackageFilter] = {
    for {
      pkg <- PackageRepository.exists(namespace, packageId)
      filter = PackageFilter(namespace, pkg.uuid, name)
      _ <- FilterRepository.exists(namespace, name)
      _ <- packageFilters += filter
    } yield filter
  }

  def listPackagesForFilter(namespace: Namespace, fname: Filter.Name)
                           (implicit db: Database, ec: ExecutionContext): DBIO[Seq[Package]] =

    FilterRepository.exists(namespace, fname) andThen {
      val q = for {
        ps <- PackageRepository.packages if ps.namespace === namespace
        pf <- packageFilters if pf.filterName === fname && pf.packageUuid === ps.uuid
      } yield ps
      q.result
    }

  def listFiltersForPackage(namespace: Namespace, pkgId: PackageId)
                           (implicit ec: ExecutionContext): DBIO[Seq[Filter]] = {
    val q = for {
      pkg <- PackageRepository.packages if pkg.namespace === namespace && pkg.name === pkgId.name &&
      pkg.version === pkgId.version
      pf <- packageFilters if pf.packageUuid === pkg.uuid
      fs <- FilterRepository.filters.filter(i => i.namespace === namespace && i.name === pf.filterName)
    } yield fs

    PackageRepository.exists(namespace, pkgId).andThen(q.result).transactionally
  }


  /**
   * Deletes a filter by name
    *
    * @param fname   The name of the filter to delete
   * @return        A DBIO[Int] number of filters deleted
   */
  def deletePackageFilterByFilterName(namespace: Namespace, fname: Filter.Name)
                                     (implicit ec: ExecutionContext): DBIO[Int] = {
    PackageRepository.packages
      .filter(_.namespace === namespace)
      .map(_.uuid)
      .result
      .flatMap { packageUuids =>
        packageFilters
          .filter(_.packageUuid.inSet(packageUuids))
          .filter(_.filterName === fname)
          .delete
      }
      .transactionally
  }

  def deletePackageFilter(namespace: Namespace, packageId: PackageId, fname: Filter.Name)
                         (implicit ec: ExecutionContext): DBIO[Unit] =
    PackageRepository.packages
      .filter(_.namespace === namespace)
      .filter(_.name === packageId.name)
      .filter(_.version === packageId.version)
      .map(_.uuid)
      .result
      .flatMap { packageUuids =>
        packageFilters
          .filter(_.filterName === fname)
          .delete
          .handleSingleUpdateError(Errors.MissingPackageFilter)
      }
      .transactionally
}
