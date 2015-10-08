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

object PackageRepository {

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

  def add(pkg: Package): DBIO[Int] =
    packages.insertOrUpdate(pkg)

  def list: DBIO[Seq[Package]] =
    packages.result

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

  def load(ids: Set[Package.Id])
          (implicit ec: ExecutionContext): DBIO[Set[Package]] = {
    packages.filter( x =>
      x.name.mappedTo[String] ++ x.version.mappedTo[String] inSet ids.map( id => id.name.get + id.version.get )
    ).result.map( _.toSet )
  }

}

object PackageFilterRepository {

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

  def add(pf: PackageFilter)(implicit ec: ExecutionContext): DBIO[PackageFilter] =
    (packageFilters += pf).map(_ => pf)

  def list: DBIO[Seq[PackageFilter]] = packageFilters.result

  def listPackagesForFilter(fname: Filter.Name): DBIO[Seq[Package]] = {
    val q = for {
      pf <- packageFilters.filter(_.filterName === fname)
      ps <- PackageRepository.packages
              .filter(pkg => pkg.name    === pf.packageName
                          && pkg.version === pf.packageVersion)
    } yield ps
    q.result
  }

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

  def delete(fname: Filter.Name): DBIO[Int] =
    packageFilters.filter(_.filterName === fname).delete

  def deletePackageFilter(pname: Package.Name, pversion: Package.Version, fname: Filter.Name): DBIO[Int] =
    packageFilters
      .filter(pf => pf.packageName    === pname
                 && pf.packageVersion === pversion
                 && pf.filterName     === fname)
      .delete

}
