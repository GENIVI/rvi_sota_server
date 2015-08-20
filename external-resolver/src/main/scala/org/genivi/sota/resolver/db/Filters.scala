/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.types.Filter
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


object Filters {

  // scalastyle:off
  class FiltersTable(tag: Tag) extends Table[Filter](tag, "Filter") {

    def name       = column[Filter.Name]("name", O.PrimaryKey)
    def expression = column[Filter.Expression]("expression")

    def * = (name, expression) <> ((Filter.apply _).tupled, Filter.unapply)
  }
  // scalastyle:on

  val filters = TableQuery[FiltersTable]

  def add(filter: Filter)(implicit ec: ExecutionContext): DBIO[Filter] =
    (filters += filter).map(_ => filter)

  def list: DBIO[Seq[Filter]] =
    filters.result
}
