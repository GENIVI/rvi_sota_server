/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.resolve

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import org.genivi.sota.data.PackageId
import org.genivi.sota.data.Namespace._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPackageId
import org.genivi.sota.resolver.vehicles.VehicleRepository

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import Directives._
import org.genivi.sota.datatype.NamespaceDirective

/**
 * API routes for package resolution.
 */
class ResolveDirectives(namespaceExtractor: Directive1[Namespace] = NamespaceDirective.defaultNamespaceExtractor)
                       (implicit system: ActorSystem,
                        db: Database,
                        mat: ActorMaterializer,
                        ec: ExecutionContext) {

  def resolvePackage(ns: Namespace, id: PackageId): Route =
    completeOrRecoverWith(db.run(VehicleRepository.resolve(ns, id))) {
      Errors.onMissingPackage
    }

  /**
   * API route for resolving a package, i.e. returning the list of VINs it applies to.
   * @return Route object containing route for package resolution
   * @throws Errors.MissingPackageException if the package doesn't exist
   */
  def route: Route =
    (get & pathPrefix("resolve") & namespaceExtractor & refinedPackageId) { (ns, id) =>
      resolvePackage(ns, id)
    }

}
