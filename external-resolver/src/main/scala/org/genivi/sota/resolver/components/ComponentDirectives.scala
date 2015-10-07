/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.components

import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPartNumber
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend.Database
import Directives._

class ComponentDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {

  def route: Route = {
    pathPrefix("components") {
      get {
        complete {
          NoContent
        }
      } ~
      (put & refinedPartNumber & entity(as[Component.DescriptionWrapper]))
      { (part, desc) =>
          val comp = Component(part, desc.description)
          complete(db.run(ComponentRepository.addComponent(comp)).map(_ => comp))
      }
    }
  }

}
