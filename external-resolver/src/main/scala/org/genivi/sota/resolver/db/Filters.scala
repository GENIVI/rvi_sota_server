/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

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

  class MissingFilterException extends Throwable with NoStackTrace

  def update(filter: Filter)(implicit ec: ExecutionContext): DBIO[Filter] = {
    val q = for {
      f <- filters if f.name === filter.name
    } yield f.expression
    q.update(filter.expression).map(i => if (i == 0) throw new MissingFilterException else filter)
  }

  def deleteFilter(name: Filter.Name)(implicit ec: ExecutionContext): DBIO[String] =
    filters
      .filter(_.name === name)
      .delete
      .map(i =>
        if (i == 0)
          throw new MissingFilterException
        else s"The filter named $name has been deleted.")

  def delete(name: Filter.Name)(implicit ec: ExecutionContext): DBIO[String] =
    PackageFilters.deleteByFilterName(name).andThen(deleteFilter(name))

  def list: DBIO[Seq[Filter]] =
    filters.result
}
