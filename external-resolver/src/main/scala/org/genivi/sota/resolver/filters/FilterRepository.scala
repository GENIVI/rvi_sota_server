/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.data.Namespace
import org.genivi.sota.db.Operators._
import org.genivi.sota.refined.SlickRefined._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.db.PackageFilterRepository

import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace
import slick.driver.MySQLDriver.api._


object FilterRepository {
  import org.genivi.sota.db.SlickExtensions._

  // scalastyle:off
  class FiltersTable(tag: Tag) extends Table[Filter](tag, "Filter") {

    def namespace  = column[Namespace]("namespace")
    def name       = column[Filter.Name]("name")
    def expression = column[Filter.Expression]("expression")

    // insertOrUpdate buggy for composite-keys, see Slick issue #966.
    def pk = primaryKey("pk_filter", (namespace, name))

    def * = (namespace, name, expression) <> ((Filter.apply _).tupled, Filter.unapply)
  }
  // scalastyle:on

  val filters = TableQuery[FiltersTable]

  def add(filter: Filter)(implicit ec: ExecutionContext): DBIO[Filter] =
    (filters += filter).map(_ => filter)

  def exists(namespace: Namespace, name: Filter.Name)(implicit ec: ExecutionContext): DBIO[Filter] =
    filters
      .filter(i => i.namespace === namespace && i.name === name)
      .result
      .headOption
      .failIfNone(Errors.MissingFilter)

  def update(filter: Filter)(implicit ec: ExecutionContext): DBIO[Filter] = {
    val q = for {
      f <- filters if f.name === filter.name
    } yield f.expression
    q.update(filter.expression)
     .flatMap(i => if (i == 0) DBIO.failed(Errors.MissingFilter) else DBIO.successful(filter))
  }

  def delete(namespace: Namespace, name: Filter.Name)(implicit ec: ExecutionContext): DBIO[Int] =
    exists(namespace, name) andThen
    filters.filter(i => i.namespace === namespace && i.name === name).delete

  def deleteFilterAndPackageFilters
    (namespace: Namespace, name: Filter.Name)
    (implicit db: Database, ec: ExecutionContext): DBIO[Unit] =
    for {
      _ <- PackageFilterRepository.deletePackageFilterByFilterName(namespace, name)
      _ <- FilterRepository.delete(namespace, name)
    } yield ()

  def list: DBIO[Seq[Filter]] =
    filters.result

  def searchByRegex(namespace: Namespace, re: Refined[String, Regex]): DBIO[Seq[Filter]] =
    filters.filter(filter => filter.namespace === namespace && regex(filter.name, re.get)).result

}
