/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core
import org.genivi.sota.core.db.BlacklistedPackages

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import org.genivi.sota.data.Namespace
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._

import slick.driver.MySQLDriver.api._
import Directives._
import org.genivi.sota.http.ErrorHandler

class ImpactResource(namespaceExtractor: Directive1[Namespace])
                    (implicit db: Database, system: ActorSystem) {
  import system.dispatcher

  def runImpactAnalysis(namespace: Namespace): Route = {
    val f = BlacklistedPackages.impact(namespace).map { _.map { case (k, v) ⇒ Map(k → v) } }
    complete(f)
  }

  val route = (ErrorHandler.handleErrors & pathPrefix("impact"))  {
    (get & path("blacklist") & namespaceExtractor) { ns =>
      runImpactAnalysis(ns)
    }
  }
}
