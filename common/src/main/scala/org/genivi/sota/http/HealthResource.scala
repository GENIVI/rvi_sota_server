/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.http

import akka.http.scaladsl.server.Directives
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import io.circe.syntax._

class HealthResource(db: Database, versionRepr: Map[String, Any] = Map.empty)
                    (implicit val ec: ExecutionContext) {
  import Directives._

  private def dbVersion(): Future[String] = {
    val query = sql"SELECT VERSION()".as[String].head
    db.run(query)
  }

  val route =
    (get & pathPrefix("health")) {
      pathEnd {
        val query = sql"SELECT 1 FROM dual ".as[Int]
        val f = db.run(query).map(_ => Map("status" -> "OK"))
        complete(f)
      } ~
      path("version") {
        val f = dbVersion().map { v =>
          (versionRepr.mapValues(_.toString) + ("dbVersion" -> v)).asJson
        }

        complete(f)
      }
    }
}
