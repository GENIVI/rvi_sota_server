/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import org.genivi.sota.resolver.filters.FilterDirectives
import org.genivi.sota.resolver.packages.PackageDirectives
import org.genivi.sota.resolver.resolve.ResolveDirectives
import org.genivi.sota.resolver.vehicles.VehicleDirectives
import org.genivi.sota.resolver.components.ComponentDirectives
import org.genivi.sota.rest.Handlers.{rejectionHandler, exceptionHandler}
import scala.concurrent.ExecutionContext
import scala.util.Try
import slick.jdbc.JdbcBackend.Database

/**
 * Base API routing class.
 * @see {@linktourl http://pdxostc.github.io/rvi_sota_server/dev/api.html}
 */
class Routing
  (implicit db: Database, system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext)
    extends Directives {

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        new VehicleDirectives().route ~
        new PackageDirectives().route ~
        new FilterDirectives().route ~
        new ResolveDirectives().route ~
        new ComponentDirectives().route
      }
    }
  }
}

object Boot extends App {

  implicit val system       = ActorSystem("sota-external-resolver")
  implicit val materializer = ActorMaterializer()
  implicit val exec         = system.dispatcher
  implicit val log          = Logging(system, "boot")
  implicit val db           = Database.forConfig("database")

  log.info(org.genivi.sota.resolver.BuildInfo.toString)

  val route         = new Routing
  val host          = system.settings.config.getString("server.host")
  val port          = system.settings.config.getInt("server.port")
  val bindingFuture = Http().bindAndHandle(route.route, host, port)

  log.info(s"Server online at http://${host}:${port}/")

  sys.addShutdownHook {
    Try( db.close()  )
    Try( system.terminate() )
  }
}
