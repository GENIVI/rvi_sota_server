/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.components

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPartNumber
import org.genivi.sota.resolver.common.Errors
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcBackend.Database
import Directives._

/**
 * API routes for creating, deleting, and listing components.
 * @see {@linktourl http://pdxostc.github.io/rvi_sota_server/dev/api.html} 
 */
class ComponentDirectives(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {

  /**
   * API route for filters.
   * @return      Route object containing routes for creating, editing, and listing components
   * @throws      Errors.ComponentIsInstalledException on DELETE call, if component doesn't exist
   */
  def route: Route = {
    pathPrefix("components") {
      get {
        parameter('regex.as[Refined[String, Regex]].?) { re =>
          val query = re.fold(ComponentRepository.list)(re => ComponentRepository.searchByRegex(re))
          complete(db.run(query))
        }
      } ~
      (put & refinedPartNumber & entity(as[Component.DescriptionWrapper]))
      { (part, desc) =>
          val comp = Component(part, desc.description)
          complete(db.run(ComponentRepository.addComponent(comp)).map(_ => comp))
      } ~
      (delete & refinedPartNumber)
      { part =>
          completeOrRecoverWith(db.run(ComponentRepository.removeComponent(part))) {
            Errors.onComponentInstalled
          }
      }
    }
  }

}
