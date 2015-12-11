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
import org.genivi.sota.resolver.packages.PackageFilterRepository
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.rest.Validation._
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcBackend.Database


/**
 * API routes for filters.
 * @see {@linktourl http://pdxostc.github.io/rvi_sota_server/dev/api.html}
 */
class FilterDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {

  /**
   * API route for filters.
   * @return      Route object containing routes for getting, creating, editing, and deleting filters
   * @throws      Errors.MissingFilterException if the filter doesn't exist
   */
  def filterRoute: Route =

    pathPrefix("filters") {
      (get & pathEnd) {
        parameter('regex.as[Refined[String, Regex]].?) { re =>
          val query = re.fold(FilterRepository.list)(re => FilterRepository.searchByRegex(re))
          complete(db.run(query))
        }
      } ~
      (get & refined[Filter.ValidName](Slash ~ Segment) & path("package")) { filter =>
        completeOrRecoverWith(db.run(PackageFilterRepository.listPackagesForFilter(filter))) {
          Errors.onMissingFilter
        }
      } ~
      (post & entity(as[Filter]) & pathEnd) { filter =>
        complete(db.run(FilterRepository.add(filter)))
      } ~
      (put & refined[Filter.ValidName](Slash ~ Segment ~ PathEnd)
           & entity(as[Filter.ExpressionWrapper])
           & pathEnd)
      { (fname, expr) =>
        complete(db.run(FilterRepository.update(Filter(fname, expr.expression))))
      } ~
      (delete & refined[Filter.ValidName](Slash ~ Segment ~ PathEnd) & pathEnd)
      { fname =>
        complete(db.run(FilterRepository.deleteFilterAndPackageFilters(fname)))
      }

    }

  /**
   * API route for validating filters.
   * @return      Route object containing routes for verifying that a filter is valid
   */
  def validateRoute: Route = {
    pathPrefix("validate") {
      path("filter") ((post & entity(as[Filter])) (_ => complete("OK")))
    }
  }

  def route: Route =
    handleExceptions( ExceptionHandler( Errors.onMissingFilter orElse Errors.onMissingPackage ) ) {
      filterRoute ~ validateRoute
    }

}
