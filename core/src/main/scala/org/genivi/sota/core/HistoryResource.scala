/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid
import io.circe.generic.auto._
import org.genivi.sota.core.data.InstallHistory
import org.genivi.sota.core.db.InstallHistories
import org.genivi.sota.data.Device
import org.genivi.sota.data.Namespace.Namespace
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import slick.driver.MySQLDriver.api._


class HistoryResource(db: Database, namespaceExtractor: Directive1[Namespace])
                     (implicit system: ActorSystem) extends Directives {

  import Device.{Id, ValidId}
  import CirceMarshallingSupport._

  /**
    * A web app GET all install attempts, a Seq of [[InstallHistory]], for the given device
    */
  def history(ns: Namespace, device: Id): Route = {
    complete(db.run(InstallHistories.list(ns, device)))
  }

  val route =
    (pathPrefix("history") & parameter('uuid.as[String Refined ValidId]) & namespaceExtractor) { (uuid, ns) =>
      (get & pathEnd) { history(ns, Id(uuid)) }
    }
}
