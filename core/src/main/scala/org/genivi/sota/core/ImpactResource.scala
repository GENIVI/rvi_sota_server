/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directive1, Route}
import org.genivi.sota.core.db.BlacklistedPackages
import org.genivi.sota.data.Namespace
import slick.driver.MySQLDriver.api._
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import io.circe.generic.auto._

class ImpactResource(namespaceExtractor: Directive1[Namespace])
                    (implicit db: Database, system: ActorSystem) {

  import akka.http.scaladsl.server.Directives._

  import system.dispatcher

  def runImpactAnalysis(namespace: Namespace): Route = {
    val f = BlacklistedPackages.impact(namespace)
    complete(f)
  }

  val route = (ErrorHandler.handleErrors & pathPrefix("impact"))  {
    (get & path("blacklist") & namespaceExtractor) { ns =>
      runImpactAnalysis(ns)
    }
  }
}
