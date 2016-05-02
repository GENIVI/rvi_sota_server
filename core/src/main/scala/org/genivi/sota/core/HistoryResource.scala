/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directives, Route}
import org.genivi.sota.core.db.InstallHistories
import org.genivi.sota.data.Namespace.Namespace
import org.genivi.sota.data.Vehicle.Vin
import slick.driver.MySQLDriver.api._
import akka.http.scaladsl.marshalling.Marshaller._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import io.circe.generic.auto._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._


class HistoryResource (db: Database)(implicit system: ActorSystem) extends Directives {
  import org.genivi.sota.core.WebService._
  import org.genivi.sota.core.common.NamespaceDirective._
  import CirceMarshallingSupport._

  def history(ns: Namespace, vin: Vin): Route = {
    complete(db.run(InstallHistories.list(ns, vin)))
  }

  val route =
    (pathPrefix("history") & parameter('vin.as[Vin])) { vin =>
      extractNamespace(system) { ns =>
        (get & pathEnd) { history(ns, vin) }
      }
    }
}
