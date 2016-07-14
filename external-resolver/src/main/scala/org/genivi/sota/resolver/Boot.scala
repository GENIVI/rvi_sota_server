/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import org.genivi.sota.http.{HealthResource, NamespaceDirectives, TraceId}

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import org.genivi.sota.data.Namespace
import org.genivi.sota.resolver.filters.FilterDirectives
import org.genivi.sota.resolver.packages.{PackageDirectives, PackageFiltersResource}
import org.genivi.sota.resolver.resolve.ResolveDirectives
import org.genivi.sota.resolver.vehicles.VehicleDirectives
import org.genivi.sota.resolver.components.ComponentDirectives
import org.genivi.sota.rest.Handlers.{exceptionHandler, rejectionHandler}
import org.slf4j.LoggerFactory

import scala.util.Try
import org.genivi.sota.http.LogDirectives._
import slick.driver.MySQLDriver.api._


/**
 * Base API routing class.
  *
  * @see {@linktourl http://advancedtelematic.github.io/rvi_sota_server/dev/api.html}
 */
class Routing(namespaceDirective: Directive1[Namespace])
  (implicit db: Database, system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext)
 {
   import Directives._

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        new VehicleDirectives(namespaceDirective).route ~
        new PackageDirectives(namespaceDirective).route ~
        new FilterDirectives(namespaceDirective).route ~
        new ResolveDirectives(namespaceDirective).route ~
        new ComponentDirectives(namespaceDirective).route ~
        new PackageFiltersResource(namespaceDirective).routes
      }
    }
  }
}


object Boot extends App with Directives {
  import org.genivi.sota.http.VersionDirectives.versionHeaders

  implicit val system = ActorSystem("ota-plus-resolver")
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher
  implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val db = Database.forConfig("database")

  val config = system.settings.config

  if (config.getBoolean("database.migrate")) {
    val url = config.getString("database.url")
    val user = config.getString("database.properties.user")
    val password = config.getString("database.properties.password")

    import org.flywaydb.core.Flyway
    val flyway = new Flyway
    flyway.setDataSource(url, user, password)
    flyway.migrate()
  }

  lazy val version = {
    val bi = org.genivi.sota.resolver.BuildInfo
    s"${bi.name}/${bi.version}"
  }

  val namespaceDirective = NamespaceDirectives.fromConfig()

  val routes: Route =
    (TraceId.withTraceId &
      logResponseMetrics("sota-resolver", TraceId.traceMetrics) &
      versionHeaders(version)) {
      Route.seal {
        new Routing(namespaceDirective).route ~
        new HealthResource(db, org.genivi.sota.resolver.BuildInfo.toMap).route
      }
    }

  val host = config.getString("server.host")
  val port = config.getInt("server.port")
  val bindingFuture = Http().bindAndHandle(routes, host, port)

  log.info(s"sota resolver started at http://$host:$port/")

  sys.addShutdownHook {
    Try(db.close())
    Try(system.terminate())
  }
}
