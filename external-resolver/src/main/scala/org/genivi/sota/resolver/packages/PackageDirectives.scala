/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
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
import org.genivi.sota.data.{Namespace, PackageId}
import org.genivi.sota.http.AuthedNamespaceScope
import org.genivi.sota.http.ErrorHandler
import org.genivi.sota.http.Scopes
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.RefinementDirectives._
import org.genivi.sota.resolver.db.{Package, PackageFilterRepository, PackageRepository}
import org.genivi.sota.resolver.filters.Filter
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.rest.ResponseConversions._
import org.genivi.sota.resolver.db.PackageFilterResponse._

import scala.concurrent.ExecutionContext
import slick.jdbc.MySQLProfile.api._


class PackageDirectives(namespaceExtractor: Directive1[AuthedNamespaceScope], deviceRegistryClient: DeviceRegistry)
                       (implicit system: ActorSystem,
                        db: Database, mat:
                        ActorMaterializer,
                        ec: ExecutionContext) {
  import Directives._

  implicit val NamespaceUnmarshaller: FromStringUnmarshaller[Namespace] = Unmarshaller.strict(Namespace.apply)


  def ok: StandardRoute =
    complete {
      StatusCodes.NoContent
    }

  def getFilters(ns: Namespace): StandardRoute =
    complete(db.run(PackageFilterRepository.list(ns)).map(_.toResponse))

  def getPackage(ns: Namespace, id: PackageId): Route =
    complete(db.run(PackageRepository.exists(ns, id)))

  def addPackage(ns: Namespace, id: PackageId): Route =
    entity(as[Package.Metadata]) { metadata =>
      if (metadata.namespace == ns) {
        complete(db.run(PackageRepository.add(id, metadata)))
      } else {
        reject(AuthorizationFailedRejection)
      }
    }

  def getPackageFilters(ns: Namespace, id: PackageId): Route =
    complete(db.run(PackageFilterRepository.listFiltersForPackage(ns, id)))

  def addPackageFilter(ns: Namespace, id: PackageId, fname: String Refined Filter.ValidName): Route =
    complete(db.run(PackageFilterRepository.addPackageFilter(ns, id, fname)).map(_.toResponse(id)))

  def deletePackageFilter(ns: Namespace, id: PackageId, fname: String Refined Filter.ValidName): Route =
    complete(db.run(PackageFilterRepository.deletePackageFilter(ns, id, fname)))

  def packageFilterApi(ns: AuthedNamespaceScope, id: PackageId): Route = {
    val scope = Scopes.resolver(ns)
    (scope.get & pathEnd) {
      getPackageFilters(ns, id)
    } ~
    (scope.put & refinedFilterName & pathEnd) { fname =>
      addPackageFilter(ns, id, fname)
    } ~
    (scope.delete & refinedFilterName & pathEnd) { fname =>
      deletePackageFilter(ns, id, fname)
    }
  }

  /**
   * API route for packages.
   *
   * @return      Route object containing route for adding packages
   */
  def route: Route = (ErrorHandler.handleErrors & namespaceExtractor) { ns =>
    val scope = Scopes.resolver(ns)
    pathPrefix("packages") {
      (scope.get & path("filter")) {
        getFilters(ns)
      } ~
      ((scope.get | scope.put | scope.delete) & refinedPackageId) { id =>
        (scope.get & pathEnd) {
          getPackage(ns, id)
        } ~
        (scope.put & pathEnd) {
          addPackage(ns, id)
        } ~
        pathPrefix("filter") { packageFilterApi(ns, id) }
      }
    }
  }
}
