/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.resolve

import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPackageId
import org.genivi.sota.resolver.resolve._
import org.genivi.sota.rest.Validation._
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend.Database


class ResolveDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {
  import Directives._

  def route(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Route = {
    pathPrefix("resolve") {
      (get & refinedPackageId) { id =>
        completeOrRecoverWith(ResolveFunctions.resolve(id)) {
          Errors.onMissingPackage
        }
      }
    }
  }
}
