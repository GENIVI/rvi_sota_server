/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.resolver.types.Filter

import scala.concurrent.ExecutionContext

object Filters {
  import slick.driver.MySQLDriver.api._

  // scalastyle:off
  class FiltersTable(tag: Tag) extends Table[Filter](tag, "Filter") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def expression = column[String]("expression")

    def * = (id.?, name, expression) <>
      ((Filter.apply _).tupled, Filter.unapply)
  }
  // scalastyle:on

  val filters = TableQuery[FiltersTable]

  def add(filter: Filter.ValidFilter)(implicit ec: ExecutionContext): DBIO[Filter] =
    (filters
      returning filters.map(_.id)
      into ((filter, id) => filter.copy(id = Some(id)))) += filter

  def list: DBIO[Seq[Filter]] =
    filters.result
}
