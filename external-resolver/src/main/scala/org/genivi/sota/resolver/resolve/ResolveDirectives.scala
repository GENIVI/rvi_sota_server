/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.resolve

import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPackageId
import org.genivi.sota.resolver.vehicles.VehicleRepository
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend.Database
import Directives._


class ResolveDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {

  def route(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Route = {
    pathPrefix("resolve") {
      (get & refinedPackageId) { id =>
        completeOrRecoverWith(db.run(VehicleRepository.resolve(id))) {
          Errors.onMissingPackage
        }
      }
    }
  }
}
