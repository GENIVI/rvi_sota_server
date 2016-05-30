/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import org.genivi.sota.core.db.InstallHistories
import org.genivi.sota.data.Namespace.Namespace
import org.genivi.sota.data.Vehicle.Vin
import slick.driver.MySQLDriver.api._
import akka.http.scaladsl.marshalling.Marshaller._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import io.circe.generic.auto._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.core.data.InstallHistory
import org.genivi.sota.datatype.NamespaceDirective

class HistoryResource(db: Database, namespaceExtractor: Directive1[Namespace] = NamespaceDirective.defaultNamespaceExtractor)
                     (implicit system: ActorSystem) extends Directives {

  import CirceMarshallingSupport._

  /**
    * An ota client GET all install attempts, a Seq of [[InstallHistory]], for the given VIN
    */
  def history(ns: Namespace, vin: Vin): Route = {
    complete(db.run(InstallHistories.list(ns, vin)))
  }

  val route =
    (pathPrefix("history") & parameter('vin.as[Vin]) & namespaceExtractor) { (vin, ns) =>
      (get & pathEnd) { history(ns, vin) }
    }
}
