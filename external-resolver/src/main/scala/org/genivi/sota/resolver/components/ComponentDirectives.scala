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

  def searchComponent =
    parameter('regex.as[String Refined Regex].?) { re =>
      val query = re.fold(ComponentRepository.list)(re => ComponentRepository.searchByRegex(re))
      complete(db.run(query))
    }

  def addComponent(part: Component.PartNumber) =
    entity(as[Component.DescriptionWrapper]) { descr =>
      val comp = Component(part, descr.description)
      complete(db.run(ComponentRepository.addComponent(comp)).map(_ => comp))
    }


  def deleteComponent(part: Component.PartNumber) =
    completeOrRecoverWith(db.run(ComponentRepository.removeComponent(part))) {
      Errors.onComponentInstalled
    }

  /**
   * API route for components.
   * @return      Route object containing routes for creating, editing, and listing components
   * @throws      Errors.ComponentIsInstalledException on DELETE call, if component doesn't exist
   */
  def route: Route =
    pathPrefix("components") {
      (get & pathEnd) {
        searchComponent
      } ~
      (put & refinedPartNumber & pathEnd) { part =>
        addComponent(part)
      } ~
      (delete & refinedPartNumber & pathEnd) { part =>
        deleteComponent(part)
      }
    }

}
