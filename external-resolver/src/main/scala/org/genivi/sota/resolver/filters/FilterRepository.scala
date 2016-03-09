/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.db.Operators._
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.packages.PackageFilterRepository
import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace
import slick.driver.MySQLDriver.api._


object FilterRepository {

  // scalastyle:off
  class FiltersTable(tag: Tag) extends Table[Filter](tag, "Filter") {

    def name       = column[Filter.Name]("name")
    def expression = column[Filter.Expression]("expression")

    def pk = primaryKey("pk_filter", name)

    def * = (name, expression) <> ((Filter.apply _).tupled, Filter.unapply)
  }
  // scalastyle:on

  val filters = TableQuery[FiltersTable]

  def add(filter: Filter)(implicit ec: ExecutionContext): DBIO[Filter] =
    (filters += filter).map(_ => filter)

  def exists(name: Filter.Name)(implicit ec: ExecutionContext): DBIO[Filter] =
    filters
      .filter(_.name === name)
      .result
      .headOption
      .failIfNone(Errors.MissingFilterException)

  def update(filter: Filter)(implicit ec: ExecutionContext): DBIO[Filter] = {
    val q = for {
      f <- filters if f.name === filter.name
    } yield f.expression
    q.update(filter.expression)
     .flatMap(i => if (i == 0) DBIO.failed(Errors.MissingFilterException) else DBIO.successful(filter))
  }

  def delete(name: Filter.Name)(implicit ec: ExecutionContext): DBIO[Int] =
    exists(name) andThen
    filters.filter(_.name === name).delete

  def deleteFilterAndPackageFilters
    (name: Filter.Name)
    (implicit db: Database, ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- PackageFilterRepository.deletePackageFilterByFilterName(name)
      _ <- FilterRepository.delete(name)
    } yield ()

  def list: DBIO[Seq[Filter]] =
    filters.result

  def searchByRegex(re: Refined[String, Regex]): DBIO[Seq[Filter]] =
    filters.filter(filter => regex(filter.name, re.get)).result

}
