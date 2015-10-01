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
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.Errors
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPackageId
import org.genivi.sota.rest.Validation._
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend.Database


class PackageDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {
  import Directives._

  def route(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Route = {

    pathPrefix("packages") {
      get {
        complete {
          NoContent
        }
      } ~
      (put & refinedPackageId & entity(as[Package.Metadata]))
      { (id, metadata) =>
        val pkg = Package(id, metadata.description, metadata.vendor)
        complete(db.run(PackageDAO.add(pkg).map(_ => pkg)))
      }
    }
  }
}
