/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server._
import cats.data.{Xor, XorT}
import io.circe.{Encoder, Decoder, Json}
import Json.{obj, string}
import org.genivi.sota.resolver.db.Filters.filters
import org.genivi.sota.resolver.packages._
import org.genivi.sota.resolver.types.PackageFilter
import org.genivi.sota.resolver.types.{Filter}
import scala.concurrent.{ExecutionContext, Future}
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

  def add(pf: PackageFilter)(implicit ec: ExecutionContext): DBIO[PackageFilter] =
    (packageFilters += pf).map(_ => pf)

  def list: DBIO[Seq[PackageFilter]] = packageFilters.result

  def listPackagesForFilter(fname: Filter.Name)
                           (implicit ec: ExecutionContext): DBIO[Seq[Package]] = {
    val q = for {
      pf <- packageFilters.filter(_.filterName === fname)
      ps <- PackageDAO.packages.filter(pkg => pkg.name === pf.packageName && pkg.version === pf.packageVersion)
    } yield ps
    q.result
  }

  def listFiltersForPackage(packageId: Package.Id)
                           (implicit ec: ExecutionContext): DBIO[(Option[Package], Seq[Filter])] = {
    val qFilters = for {
      pf  <- packageFilters if pf.packageName === packageId.name && pf.packageVersion === packageId.version
      f   <- Filters.filters if f.name === pf.filterName
    } yield f

    for {
      p  <- PackageDAO.packages.filter( x => x.name === packageId.name && x.version === packageId.version ).result.headOption
      fs <- qFilters.result
    } yield (p, fs)
  }

  def delete(fname: Filter.Name)(implicit ec: ExecutionContext): DBIO[Int] =
    packageFilters.filter(_.filterName === fname).delete

  def delete(pname: Package.Name, pversion: Package.Version, fname: Filter.Name)
    (implicit ec: ExecutionContext) : DBIO[Int] =
    packageFilters
      .filter(pf => pf.packageName    === pname
                 && pf.packageVersion === pversion
                 && pf.filterName     === fname)
      .delete
}
