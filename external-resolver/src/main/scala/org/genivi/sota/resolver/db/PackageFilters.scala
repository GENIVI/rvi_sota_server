/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server._
import org.genivi.sota.resolver.db.Filters.filters
import org.genivi.sota.resolver.db.Packages.packages
import org.genivi.sota.resolver.types.PackageFilter
import org.genivi.sota.resolver.types.{Filter, Package}
import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace
import scala.util.{Try, Success, Failure}
import slick.driver.MySQLDriver.api._


object PackageFilters {

  import org.genivi.sota.refined.SlickRefined._

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

  class MissingPackageException extends Throwable with NoStackTrace

  def add(pf: PackageFilter)(implicit ec: ExecutionContext): DBIO[PackageFilter] = {

    def failIfEmpty[X](io: DBIO[Seq[X]], t: => Throwable)(implicit ec: ExecutionContext): DBIO[Unit] =
      io flatMap { xs => if (xs.isEmpty) DBIO.failed(t) else DBIO.successful(()) }

    for {
      _ <- failIfEmpty(packages.filter(p => p.name === pf.packageName
               && p.version === pf.packageVersion).result,
             new MissingPackageException)
      _ <- failIfEmpty(filters.filter(_.name === pf.filterName).result, new Filters.MissingFilterException)
      _ <- packageFilters += pf
    } yield pf
  }

  def list: DBIO[Seq[PackageFilter]] =
    packageFilters.result

  def listPackagesForFilter(fname: Filter.Name)(implicit ec: ExecutionContext): DBIO[Seq[Package.Name]] =
    packageFilters.filter(_.filterName === fname).map(_.packageName).result

  def listFiltersForPackage
    (pname: Package.Name, pversion: Package.Version)
    (implicit ec: ExecutionContext)
      : DBIO[Seq[Filter]]
  = packageFilters
      .filter(p => p.packageName === pname && p.packageVersion === pversion)
      .map(_.filterName)
      .flatMap(fname => filters.filter(_.name === fname))
      .result

  def deleteByFilterName(fname: Filter.Name)(implicit ec: ExecutionContext): DBIO[Int] =
    packageFilters
      .filter(_.filterName === fname)
      .delete

  class MissingPackageFilterException extends Throwable with NoStackTrace

  def delete
    (pname: Package.Name, pversion: Package.Version, fname: Filter.Name)
    (implicit ec: ExecutionContext)
      : DBIO[Int]
  = packageFilters
      .filter(pf => pf.packageName    === pname
                 && pf.packageVersion === pversion
                 && pf.filterName     === fname)
      .delete
      .map(i =>
        if (i == 0) throw new MissingPackageFilterException else i)

}
