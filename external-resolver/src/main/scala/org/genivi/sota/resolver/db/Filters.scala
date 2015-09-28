/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.db.Operators.regex
import cats.data.Xor
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.types.Filter
import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace
import slick.driver.MySQLDriver.api._

object Filters {

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

  def load(name: Filter.Name)(implicit ec: ExecutionContext): DBIO[Option[Filter]] =
    filters
      .filter(_.name === name)
      .result.headOption

  def update(filter: Filter)(implicit ec: ExecutionContext): DBIO[Option[Filter]] = {
    val q = for {
      f <- filters if f.name === filter.name
    } yield f.expression
    q.update(filter.expression).map(i => if (i == 0) None else Some(filter))
  }

  def deleteFilter(name: Filter.Name)(implicit ec: ExecutionContext): DBIO[Int] =
    filters.filter(_.name === name).delete

  def delete(name: Filter.Name)(implicit ec: ExecutionContext): DBIO[Int] =
    (for {
      _ <- PackageFilters.delete(name)
      filtersDeleted <- deleteFilter(name)
    } yield filtersDeleted).transactionally

  def list: DBIO[Seq[Filter]] =
    filters.result

  def searchByRegex(re:String): DBIO[Seq[Filter]] =
    filters.filter(filter => regex(filter.name, re)).result

}
