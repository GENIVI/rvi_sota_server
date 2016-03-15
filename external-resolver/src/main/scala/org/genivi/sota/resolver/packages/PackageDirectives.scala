/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import org.genivi.sota.data.PackageId
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPackageId
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.rest.Validation.refined
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend.Database


class PackageDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {
  import Directives._

  /**
   * API route for packages.
 *
   * @return      Route object containing route for adding packages
   */
  def route: Route = {

    pathPrefix("packages") {
      (get & pathEnd) {
        complete {
          StatusCodes.NoContent
        }
      } ~
      (get & path("filter")) {
        complete(db.run(PackageFilterRepository.list))
      } ~
      ((get | put | delete) & refinedPackageId) { id =>
        (get & pathEnd) {
          completeOrRecoverWith(db.run(PackageRepository.exists(id))) {
            Errors.onMissingPackage
          }
        } ~
        (put & entity(as[Package.Metadata]) & pathEnd) { metadata =>
          val pkg = Package(id, metadata.description, metadata.vendor)
          complete(db.run(PackageRepository.add(pkg).map(_ => pkg)))
        } ~
        packageFilterRoute(id)
      }
    }
  }

  def packageFilterRoute(id: PackageId): Route = {

    pathPrefix("filter") {
      (get & pathEnd) {
        completeOrRecoverWith(db.run(
          PackageFilterRepository.listFiltersForPackage(id))) {
            Errors.onMissingPackage
          }
      } ~
      (put & refined[Filter.ValidName](Slash ~ Segment ~ PathEnd)) { fname =>
        completeOrRecoverWith(db.run(
          PackageFilterRepository.addPackageFilter(PackageFilter(id.name, id.version, fname)))) {
            Errors.onMissingPackage orElse Errors.onMissingFilter orElse { case err => throw(err) }
          }
      } ~
      (delete & refined[Filter.ValidName] (Slash ~ Segment ~ PathEnd)) { fname =>
        completeOrRecoverWith(db.run(
          PackageFilterRepository.deletePackageFilter(PackageFilter(id.name, id.version, fname)))) {
            case PackageFilterRepository.MissingPackageFilterException =>
              complete(StatusCodes.NotFound ->
                ErrorRepresentation( ErrorCode("filter_not_found"),
                  s"No filter with the name '${fname.get}' defined for package ${id.name}-${id.version}" ))
            case e                                                     => failWith(e)
        }
      }
    }

  }

}
