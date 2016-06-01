/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.http

import akka.http.scaladsl.server.Directives
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import io.circe.syntax._

class HealthResource(db: Database, versionRepr: Map[String, Any] = Map.empty)
                    (implicit val ec: ExecutionContext) {
  import Directives._

  val route =
    (get & pathPrefix("health")) {
      pathEnd {
        val query = sql"SELECT 1 FROM dual ".as[Int]
        val f = db.run(query).map(_ => Map("status" -> "OK"))
        complete(f)
      } ~
      path("version") {
        complete(versionRepr.mapValues(_.toString).asJson)
      }
    }
}
