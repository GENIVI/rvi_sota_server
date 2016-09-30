/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import cats.data.Xor
import org.genivi.sota.data.Namespace
import org.genivi.sota.db.BootMigrations
import org.genivi.sota.http._
import org.genivi.sota.messaging.{MessageBus, MessageBusPublisher}
import org.genivi.sota.rest.SotaRejectionHandler.rejectionHandler
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.Try
import slick.driver.MySQLDriver.api._

/**
 * Base API routing class.
 * @see {@linktourl http://advancedtelematic.github.io/rvi_sota_server/dev/api.html}
 */
class Routing(namespaceExtractor: Directive1[Namespace], messageBus: MessageBusPublisher)
  (implicit db: Database, system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext)
    extends Directives {

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      ErrorHandler.handleErrors {
        new Routes(namespaceExtractor, messageBus).route ~
        new GroupsResource(namespaceExtractor).route
      }
    }
  }
}

object Boot extends App with Directives with BootMigrations {
  import LogDirectives._
  import VersionDirectives._

  implicit val system = ActorSystem("device-registry")
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher
  implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val db = Database.forConfig("database")
  lazy val config = system.settings.config

  lazy val version = {
    val bi = org.genivi.sota.device_registry.BuildInfo
    s"${bi.name}/${bi.version}"
  }

  val authNamespace = NamespaceDirectives.fromConfig()

  lazy val messageBus =
    MessageBus.publisher(system, config) match {
      case Xor.Right(v) => v
      case Xor.Left(error) =>
        log.error("Could not initialize message bus publisher", error)
        MessageBusPublisher.ignore
    }

  val routes: Route =
    (TraceId.withTraceId &
      logResponseMetrics("device-registry", TraceId.traceMetrics) &
      versionHeaders(version)) {
      handleRejections(rejectionHandler) {
        pathPrefix("api" / "v1") {
          ErrorHandler.handleErrors {
            new Routes(authNamespace, messageBus).route ~
            new GroupsResource(authNamespace).route
          }
        } ~ new HealthResource(db, org.genivi.sota.device_registry.BuildInfo.toMap).route
      }
    }

  val host = config.getString("server.host")
  val port = config.getInt("server.port")
  val binding = Http().bindAndHandle(routes, host, port)

  log.info(s"device registry started at http://$host:$port/")

  sys.addShutdownHook {
    Try(db.close())
    Try(system.terminate())
  }
}
