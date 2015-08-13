/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.resolver.types.PackageFilter
import org.genivi.sota.resolver.types.{Filter, Package}
import scala.concurrent.ExecutionContext
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

  def list: DBIO[Seq[PackageFilter]] =
    packageFilters.result

}
