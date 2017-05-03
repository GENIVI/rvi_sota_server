/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import io.circe.generic.auto._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.db.InstallHistories
import org.genivi.sota.data.{DeviceDirectives, Uuid}
import org.genivi.sota.http.{AuthedNamespaceScope, Scopes}
import org.genivi.sota.marshalling.CirceMarshallingSupport
import slick.jdbc.MySQLProfile.api._


class HistoryResource(val deviceRegistry: DeviceRegistry, namespaceExtractor: Directive1[AuthedNamespaceScope])
                     (implicit db: Database, system: ActorSystem) extends Directives with DeviceDirectives {

  import CirceMarshallingSupport._
  import org.genivi.sota.rest.ResponseConversions._
  import org.genivi.sota.core.data.ClientInstallHistory._
  import system.dispatcher

  /**
    * A web app GET all install attempts, a Seq of [[InstallHistory]], for the given device
    */
  def history(device: Uuid): Route = {
    val f = db.run(InstallHistories.list(device)).map(_.toResponse)
    complete(f)
  }

  val route = namespaceExtractor { authedNs =>
    val scope = Scopes.updates(authedNs)

    (pathPrefix("history") & deviceQueryExtractor('uuid, authedNs)) { uuid =>
      (scope.get & pathEnd) {
        history(uuid)
      }
    }
  }
}
