/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.data.Namespace
import org.genivi.sota.data.Namespace._
import org.genivi.sota.http.ErrorHandler
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.db.PackageFilterRepository
import org.genivi.sota.rest.Validation._

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


/**
 * API routes for filters.
 * @see {@linktourl http://advancedtelematic.github.io/rvi_sota_server/dev/api.html}
 */
class FilterDirectives(namespaceExtractor: Directive1[Namespace])
                      (implicit system: ActorSystem,
                       db: Database,
                       mat: ActorMaterializer,
                       ec: ExecutionContext) {

  def searchFilter(ns: Namespace): Route =
    parameter('regex.as[String Refined Regex].?) { re =>
      val query = re.fold(FilterRepository.list)(re => FilterRepository.searchByRegex(ns, re))
      complete(db.run(query))
    }

  def getPackages(ns: Namespace, fname: String Refined Filter.ValidName): Route =
    complete(db.run(PackageFilterRepository.listPackagesForFilter(ns, fname)))

  def createFilter(ns: Namespace): Route =
    entity(as[Filter]) { filter =>
      // TODO: treat differing namespace names accordingly
      complete(db.run(FilterRepository.add(filter.copy(namespace = ns))))
    }

  def updateFilter(ns: Namespace, fname: String Refined Filter.ValidName): Route =
    entity(as[Filter.ExpressionWrapper]) { expr =>
      complete(db.run(FilterRepository.update(Filter(ns, fname, expr.expression))))
    }

  def deleteFilter(ns: Namespace, fname: String Refined Filter.ValidName): StandardRoute =
    complete(db.run(FilterRepository.deleteFilterAndPackageFilters(ns, fname)))

  /**
   * API route for validating filters.
   * @return      Route object containing routes for verifying that a filter is valid
   */
  def validateFilter: Route =
    entity(as[Filter]) { filter =>
      complete("OK")
    }

  def route: Route =
    ErrorHandler.handleErrors {
      (pathPrefix("filters") & namespaceExtractor) { ns =>
        (get & pathEnd) {
          searchFilter(ns)
        } ~
        (get & refined[Filter.ValidName](Slash ~ Segment) & path("package")) { fname =>
          getPackages(ns, fname)
        } ~
        (post & pathEnd) {
          createFilter(ns)
        } ~
        (put & refined[Filter.ValidName](Slash ~ Segment) & pathEnd) { fname =>
          updateFilter(ns, fname)
        } ~
        (delete & refined[Filter.ValidName](Slash ~ Segment) & pathEnd) { fname =>
          deleteFilter(ns, fname)
        }
      } ~
      (post & path("validate" / "filter")) {
        validateFilter
      }
    }

}
