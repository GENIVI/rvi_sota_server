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
import org.genivi.sota.resolver.packages._
import org.genivi.sota.rest.Validation._
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
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
      get {
        parameter('regex.as[Refined[String, Regex]].?) { re =>
          val query = re.fold(FilterRepository.list)(re => FilterRepository.searchByRegex(re))
          complete(db.run(query))
        }
      } ~
      (post & entity(as[Filter])) { filter =>
        complete(db.run(FilterRepository.add(filter)))
      } ~
      (put & refined[Filter.ValidName](Slash ~ Segment ~ PathEnd)
           & entity(as[Filter.ExpressionWrapper]))
      { (fname, expr) =>
        complete(db.run(FilterRepository.update(Filter(fname, expr.expression))))
      } ~
      (delete & refined[Filter.ValidName](Slash ~ Segment ~ PathEnd))
      { fname =>
        complete(db.run(FilterRepository.deleteFilterAndPackageFilters(fname)))
      }

    }

  /**
   * API route for package -> filter association.
   * @return      Route object containing routes for package -> filter association
   * @throws      Errors.MissingFilterException if the filter doesn't exist
   * @throws      Errors.MissingPackageException if the package doesn't exist
   */
  def packageFiltersRoute: Route =

    pathPrefix("packageFilters") {
      get {
        parameters('packageName.as[Package.Name].?, 'packageVersion.as[Package.Version].?, 'filter.as[Filter.Name].?) {
          case (Some(pkgName), Some(pkgVersion), None) =>
            val f: Future[Seq[Filter]] = for {
              (p, fs) <- db.run(PackageFilterRepository.listFiltersForPackage(Package.Id(pkgName, pkgVersion)))
              _       <-
                p.fold[Future[Package]](FastFuture.failed(Errors.MissingPackageException))(FastFuture.successful)
            } yield fs
            complete(f)

          case (None, None, Some(fname)) =>
            complete(db.run(PackageFilterRepository.listPackagesForFilter(fname)))
          case (None, None, None) =>
            complete(db.run(PackageFilterRepository.list))
          case _ =>
            complete(StatusCodes.NotFound)
        }
      } ~
      (post & entity(as[PackageFilter])) { pf =>
        complete(db.run(PackageFilterRepository.addPackageFilter(pf)))
      } ~
      (delete & refined[Package.ValidName]   (Slash ~ Segment)
              & refined[Package.ValidVersion](Slash ~ Segment)
              & refined[Filter.ValidName]    (Slash ~ Segment ~ PathEnd)) { (pname, pversion, fname) =>
        completeOrRecoverWith(db.run(
          PackageFilterRepository.deletePackageFilter(PackageFilter(pname, pversion, fname)))) {
            case PackageFilterRepository.MissingPackageFilterException =>
              complete(StatusCodes.NotFound ->
                ErrorRepresentation( ErrorCode("filter_not_found"),
                  s"No filter with the name '$fname' defined for package $pname-$pversion" ))
            case e                                              => failWith(e)
        }
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

  /**
   * Exception handler for filter routes.
   */
  def route: Route =
    handleExceptions( ExceptionHandler( Errors.onMissingFilter orElse Errors.onMissingPackage ) ) {
      filterRoute ~ packageFiltersRoute ~ validateRoute
    }

}
