/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
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
import org.genivi.sota.data.Namespace
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import slick.driver.MySQLDriver.api._


class HistoryResource(db: Database)
                     (implicit system: ActorSystem) extends Directives {

  import Device.{Id, ValidId}
  import CirceMarshallingSupport._
  import org.genivi.sota.core.data.client.ResponseConversions._
  import org.genivi.sota.core.data.ClientInstallHistory._

  /**
    * A web app GET all install attempts, a Seq of [[InstallHistory]], for the given device
    */
  def history(device: Id): Route = {
    extractExecutionContext { implicit ec =>
      val f = db.run(InstallHistories.list(device)).map(_.toResponse)
      complete(f)
    }
  }

  val route =
    (pathPrefix("history") & parameter('uuid.as[String Refined Device.ValidId])) { uuid =>
      (get & pathEnd) {
        history(Device.Id(uuid))
      }
    }
}
