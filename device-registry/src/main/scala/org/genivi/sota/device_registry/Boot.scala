/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import org.genivi.sota.data.Namespace.Namespace
import org.genivi.sota.datatype.NamespaceDirective
import org.genivi.sota.device_registry.common.DeviceRegistryErrors
import org.genivi.sota.rest.Handlers.{exceptionHandler, rejectionHandler}

import scala.concurrent.ExecutionContext
import scala.util.Try
import slick.driver.MySQLDriver.api._


/**
 * Base API routing class.
 * @see {@linktourl http://pdxostc.github.io/rvi_sota_server/dev/api.html}
 */
class Routing(namespaceExtractor: Directive1[Namespace])
  (implicit db: Database, system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext)
    extends Directives {

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler orElse DeviceRegistryErrors.errorHandler) {
        new Routes(namespaceExtractor).route
      }
    }
  }
}

object Boot extends App {

  implicit val system       = ActorSystem("sota-device-registry")
  implicit val materializer = ActorMaterializer()
  implicit val exec         = system.dispatcher
  implicit val log          = Logging(system, "boot")
  implicit val db           = Database.forConfig("database")
  val config = system.settings.config

  log.info(org.genivi.sota.device_registry.BuildInfo.toString)

  // Database migrations
  if (config.getBoolean("database.migrate")) {
    val url = config.getString("database.url")
    val user = config.getString("database.properties.user")
    val password = config.getString("database.properties.password")

    import org.flywaydb.core.Flyway
    val flyway = new Flyway
    flyway.setDataSource(url, user, password)
    flyway.migrate()
  }

  val route         = new Routing(NamespaceDirective.defaultNamespaceExtractor)
  val host          = system.settings.config.getString("server.host")
  val port          = system.settings.config.getInt("server.port")
  val bindingFuture = Http().bindAndHandle(route.route, host, port)

  log.info(s"Server online at http://$host:$port/")

  sys.addShutdownHook {
    Try(db.close())
    Try(system.terminate())
  }
}
