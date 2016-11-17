/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.components

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPartNumber

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import Directives._
import org.genivi.sota.data.Namespace
import org.genivi.sota.http.AuthedNamespaceScope
import org.genivi.sota.http.ErrorHandler

/**
 * API routes for creating, deleting, and listing components.
 * @see {@linktourl http://advancedtelematic.github.io/rvi_sota_server/dev/api.html}
 */
class ComponentDirectives(namespaceExtractor: Directive1[AuthedNamespaceScope])
                         (implicit system: ActorSystem,
                          db: Database,
                          mat: ActorMaterializer,
                          ec: ExecutionContext) {

  def searchComponent(ns: Namespace): Route =
    parameter('regex.as[String Refined Regex].?) { re =>
      val query = re.fold(ComponentRepository.list(ns))(re => ComponentRepository.searchByRegex(ns, re))
      complete(db.run(query))
    }

  def addComponent(ns: Namespace, part: Component.PartNumber): Route =
    entity(as[Component.DescriptionWrapper]) { descr =>
      val comp = Component(ns, part, descr.description)
      complete(db.run(ComponentRepository.addComponent(comp)).map(_ => comp))
    }


  def deleteComponent(ns: Namespace, part: Component.PartNumber): Route =
    complete(ComponentRepository.removeComponent(ns, part))

  def route: Route = ErrorHandler.handleErrors {
    (pathPrefix("components") & namespaceExtractor) { ns =>
      (get & pathEnd) {
        searchComponent(ns)
      } ~
        (put & refinedPartNumber & pathEnd) { part =>
          addComponent(ns, part)
        } ~
        (delete & refinedPartNumber & pathEnd) { part =>
          deleteComponent(ns, part)
        }
    }
  }
}
