/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.PackageId
import org.genivi.sota.datatype.NamespaceDirective
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.common.RefinementDirectives._
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


class PackageDirectives(namespaceExtractor: Directive1[Namespace])
                       (implicit system: ActorSystem,
                        db: Database, mat:
                        ActorMaterializer,
                        ec: ExecutionContext) {
  import Directives._

  def ok: StandardRoute =
    complete {
      StatusCodes.NoContent
    }

  def getFilters: StandardRoute =
    complete(db.run(PackageFilterRepository.list))

  def getPackage(ns: Namespace, id: PackageId): Route =
    completeOrRecoverWith(db.run(PackageRepository.exists(ns, id))) {
      Errors.onMissingPackage
    }

  def addPackage(id: PackageId): Route =
    entity(as[Package.Metadata]) { metadata =>
      val pkg = Package(metadata.namespace, id, metadata.description, metadata.vendor)
      complete(db.run(PackageRepository.add(pkg).map(_ => pkg)))
    }

  def getPackageFilters(ns: Namespace, id: PackageId): Route =
    completeOrRecoverWith(db.run(
      PackageFilterRepository.listFiltersForPackage(ns, id))) {
        Errors.onMissingPackage
      }

  def addPackageFilter(ns: Namespace, id: PackageId, fname: String Refined Filter.ValidName): Route =
    completeOrRecoverWith(db.run(
      PackageFilterRepository.addPackageFilter(PackageFilter(ns, id.name, id.version, fname)))) {
        Errors.onMissingPackage orElse Errors.onMissingFilter orElse { case err => throw(err) }
      }

  def deletePackageFilter(ns: Namespace, id: PackageId, fname: String Refined Filter.ValidName): Route =
    completeOrRecoverWith(db.run(
      PackageFilterRepository.deletePackageFilter(PackageFilter(ns, id.name, id.version, fname)))) {
        case PackageFilterRepository.MissingPackageFilterException =>
          complete(StatusCodes.NotFound ->
            ErrorRepresentation( ErrorCode("filter_not_found"),
              s"No filter for namespace '$ns' with the name '${fname.get}'" +
              s"defined for package ${id.name}-${id.version}"))
        case e => failWith(e)
    }

  def packageFilterApi(ns: Namespace, id: PackageId): Route =
      (get & pathEnd) {
        getPackageFilters(ns, id)
      } ~
      (put & refinedFilterName & pathEnd) { fname =>
        addPackageFilter(ns, id, fname)
      } ~
      (delete & refinedFilterName & pathEnd) { fname =>
        deletePackageFilter(ns, id, fname)
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
        (get & namespaceExtractor & pathEnd) { ns =>
          getPackage(ns, id)
        } ~
        (put & pathEnd) {
          addPackage(id)
        } ~
        (namespaceExtractor & pathPrefix("filter")) { ns => packageFilterApi(ns, id) }
      }
    }
}
