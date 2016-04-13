/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import eu.timepit.refined.api.Refined
import org.genivi.sota.data.PackageId
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.common.RefinementDirectives._
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.rest.Validation.refined
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend.Database


class PackageDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {
  import Directives._

  def ok =
    complete {
      StatusCodes.NoContent
    }

  def getFilters =
    complete(db.run(PackageFilterRepository.list))

  def getPackage(id: PackageId) =
    completeOrRecoverWith(db.run(PackageRepository.exists(id))) {
      Errors.onMissingPackage
    }

  def addPackage(id: PackageId) =
    entity(as[Package.Metadata]) { metadata =>
      val pkg = Package(id, metadata.description, metadata.vendor)
      complete(db.run(PackageRepository.add(pkg).map(_ => pkg)))
    }

  def getPackageFilters(id: PackageId) =
    completeOrRecoverWith(db.run(
      PackageFilterRepository.listFiltersForPackage(id))) {
        Errors.onMissingPackage
      }

  def addPackageFilter(id: PackageId, fname: String Refined Filter.ValidName) =
    completeOrRecoverWith(db.run(
      PackageFilterRepository.addPackageFilter(PackageFilter(id.name, id.version, fname)))) {
        Errors.onMissingPackage orElse Errors.onMissingFilter orElse { case err => throw(err) }
      }

  def deletePackageFilter(id: PackageId, fname: String Refined Filter.ValidName) =
    completeOrRecoverWith(db.run(
      PackageFilterRepository.deletePackageFilter(PackageFilter(id.name, id.version, fname)))) {
        case PackageFilterRepository.MissingPackageFilterException =>
          complete(StatusCodes.NotFound ->
            ErrorRepresentation( ErrorCode("filter_not_found"),
              s"No filter with the name '${fname.get}' defined for package ${id.name}-${id.version}" ))
        case e                                                     => failWith(e)
    }

  def packageFilterApi(id: PackageId): Route =
    pathPrefix("filter") {
      (get & pathEnd) {
        getPackageFilters(id)
      } ~
      (put & refinedFilterName & pathEnd) { fname =>
        addPackageFilter(id, fname)
      } ~
      (delete & refinedFilterName & pathEnd) { fname =>
        deletePackageFilter(id, fname)
      }
    }

  /**
   * API route for packages.
   *
   * @return      Route object containing route for adding packages
   */
  def route: Route =
    pathPrefix("packages") {
      (get & pathEnd) {
        ok
      } ~
      (get & path("filter")) {
        getFilters
      } ~
      ((get | put | delete) & refinedPackageId) { id =>
        (get & pathEnd) {
          getPackage(id)
        } ~
        (put & pathEnd) {
          addPackage(id)
        } ~
        packageFilterApi(id)
      }
    }

}
