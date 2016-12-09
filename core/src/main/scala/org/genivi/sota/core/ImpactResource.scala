/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core
import org.genivi.sota.core.db.BlacklistedPackages
import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import org.genivi.sota.data.Namespace
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import slick.driver.MySQLDriver.api._
import Directives._
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.http.{AuthedNamespaceScope, ErrorHandler, Scopes}

class ImpactResource(namespaceExtractor: Directive1[AuthedNamespaceScope],
                     resolverClient: ExternalResolverClient)
                    (implicit db: Database, system: ActorSystem) {
  import system.dispatcher

  val affectedDevicesFn = (resolverClient.affectedDevices _).curried

  def runImpactAnalysis(namespace: Namespace): Route = {
    val f = BlacklistedPackages.impact(namespace, affectedDevicesFn(namespace))
    complete(f)
  }

  val route = (ErrorHandler.handleErrors & pathPrefix("impact") & namespaceExtractor)  { ns =>
    val scope = Scopes.packages(ns)
    (scope.get & path("blacklist")) {
      runImpactAnalysis(ns)
    }
  }
}
