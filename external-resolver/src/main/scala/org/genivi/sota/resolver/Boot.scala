/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.stream.ActorMaterializer
import slick.driver.PostgresDriver.api._


object Boot extends App {
  implicit val system = ActorSystem("sota-external-resolver")
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher
  implicit val log = Logging(system, "boot")
  implicit val db = Database.forConfig("postgres")

  import akka.http.scaladsl.server.Directives._

  log.info( org.genivi.sota.resolver.BuildInfo.toString )

  val route =
    path("packages") {
      get {
        complete {
          NoContent
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8081)

  log.info("Server online at http://localhost:8081/")
  log.info("Database works: " + db.toString())
}
