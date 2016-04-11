/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.packages.PackageFilterRepository
import org.genivi.sota.rest.Validation._
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcBackend.Database


/**
 * API routes for filters.
 * @see {@linktourl http://pdxostc.github.io/rvi_sota_server/dev/api.html}
 */
class FilterDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {

  def searchFilter =
    parameter('regex.as[String Refined Regex].?) { re =>
      val query = re.fold(FilterRepository.list)(re => FilterRepository.searchByRegex(re))
      complete(db.run(query))
    }

  def getPackages(fname: String Refined Filter.ValidName) =
    completeOrRecoverWith(db.run(PackageFilterRepository.listPackagesForFilter(fname))) {
      Errors.onMissingFilter
    }

  def createFilter =
    entity(as[Filter]) { filter =>
      complete(db.run(FilterRepository.add(filter)))
    }

  def updateFilter(fname: String Refined Filter.ValidName) =
    entity(as[Filter.ExpressionWrapper]) { expr =>
      complete(db.run(FilterRepository.update(Filter(fname, expr.expression))))
    }

  def deleteFilter(fname: String Refined Filter.ValidName) =
    complete(db.run(FilterRepository.deleteFilterAndPackageFilters(fname)))

  /**
   * API route for validating filters.
   * @return      Route object containing routes for verifying that a filter is valid
   */
  def validateFilter =
    entity(as[Filter]) { filter =>
      complete("OK")
    }

  /**
   * API route for filters.
   * @return      Route object containing routes for getting, creating,
   *              editing, deleting, and validating filters
   * @throws      Errors.MissingFilterException if the filter doesn't exist
   */
  def route: Route =
    handleExceptions(ExceptionHandler(Errors.onMissingFilter orElse Errors.onMissingPackage)) {
      pathPrefix("filters") {
        (get & pathEnd) {
          searchFilter
        } ~
        (get & refined[Filter.ValidName](Slash ~ Segment) & path("package")) { fname =>
          getPackages(fname)
        } ~
        (post & pathEnd) {
          createFilter
        } ~
        (put & refined[Filter.ValidName](Slash ~ Segment) & pathEnd) { fname =>
          updateFilter(fname)
        } ~
        (delete & refined[Filter.ValidName](Slash ~ Segment) & pathEnd) { fname =>
          deleteFilter(fname)
        }
      } ~
      (post & path("validate" / "filter")) {
        validateFilter
      }
    }

}
