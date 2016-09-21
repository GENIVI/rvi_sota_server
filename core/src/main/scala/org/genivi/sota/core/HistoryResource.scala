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
import org.genivi.sota.data.{Device, Namespace, Uuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import slick.driver.MySQLDriver.api._


class HistoryResource(db: Database)
                     (implicit system: ActorSystem) extends Directives {

  import CirceMarshallingSupport._
  import org.genivi.sota.rest.ResponseConversions._
  import org.genivi.sota.core.data.ClientInstallHistory._

  /**
    * A web app GET all install attempts, a Seq of [[InstallHistory]], for the given device
    */
  def history(device: Uuid): Route = {
    extractExecutionContext { implicit ec =>
      val f = db.run(InstallHistories.list(device)).map(_.toResponse)
      complete(f)
    }
  }

  val route =
    (pathPrefix("history") & parameter('uuid.as[String Refined Uuid.Valid])) { uuid =>
      (get & pathEnd) {
        history(Uuid(uuid))
      }
    }
}
