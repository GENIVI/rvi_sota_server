/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPackageId
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend.Database

/**
 * API routes for packages.
 */
class PackageDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {
  import Directives._

  /**
   * API route for packages.
   * @return      Route object containing route for adding packages
   */
  def route: Route = {

    pathPrefix("packages") {
      (get & pathEnd) {
        complete {
          NoContent
        }
      } ~
      (get & refinedPackageId)
      { id =>
        completeOrRecoverWith(db.run(PackageRepository.exists(id))) {
          Errors.onMissingPackage
        }
      } ~
      (put & refinedPackageId & entity(as[Package.Metadata]))
      { (id, metadata) =>
        val pkg = Package(id, metadata.description, metadata.vendor)
        complete(db.run(PackageRepository.add(pkg).map(_ => pkg)))
      }
    }
  }
}
