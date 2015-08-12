/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.types.{Filter, FilterId}
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


object Filters {

  implicit val filterIdColumnType = MappedColumnType.base[FilterId, Long](
    { _.id },
    { FilterId.apply }
  )


  // scalastyle:off
  class FiltersTable(tag: Tag) extends Table[Filter](tag, "Filter") {

    def id         = column[FilterId]("id", O.PrimaryKey, O.AutoInc)
    def name       = column[Filter.Name]("name")
    def expression = column[Filter.Expression]("expression")

    def * = (id.?, name, expression) <>
      ((Filter.apply _).tupled, Filter.unapply)
  }
  // scalastyle:on

  val filters = TableQuery[FiltersTable]

  def add(filter: Filter)(implicit ec: ExecutionContext): DBIO[Filter] =
    (filters
      returning filters.map(_.id)
      into ((filter, id) => filter.copy(id = Some(id)))) += filter

  def list: DBIO[Seq[Filter]] =
    filters.result
}
