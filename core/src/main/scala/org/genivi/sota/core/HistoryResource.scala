/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.core.data.InstallHistory
import org.genivi.sota.core.db.InstallHistories
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future


class HistoryResource(namespaceExtractor: Directive1[Namespace])
                     (implicit db: Database, system: ActorSystem) extends Directives {

  import CirceMarshallingSupport._
  import org.genivi.sota.rest.ResponseConversions._
  import org.genivi.sota.core.data.ClientInstallHistory._
  import WebService.allowExtractor
  import system.dispatcher

  /**
    * A web app GET all install attempts, a Seq of [[InstallHistory]], for the given device
    */
  def history(device: Uuid): Route = {
    val f = db.run(InstallHistories.list(device)).map(_.toResponse)
    complete(f)
  }

  private val deviceUuid = allowExtractor(namespaceExtractor,
    parameter('uuid.as[String Refined Uuid.Valid]).map(Uuid(_)), deviceAllowed)

  private def deviceAllowed(device: Uuid): Future[Namespace] = {
    db.run(InstallHistories.deviceNamespace(device))
  }

  val route =
    (pathPrefix("history") & deviceUuid) { uuid =>
      (get & pathEnd) {
        history(uuid)
      }
    }
}
