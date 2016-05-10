/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.http

import akka.http.scaladsl.server.Directives
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

class HealthResource(db: Database)(implicit val ec: ExecutionContext) {
  import Directives._

  val route = path("health") {
    val query = sql"SELECT 1 FROM dual ".as[Int]
    val f = db.run(query).map(_ => "OK")
    complete(f)
  }
}
