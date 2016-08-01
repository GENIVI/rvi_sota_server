/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
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
import org.genivi.sota.messaging.{MessageBusManager, MessageBusPublisher}
import org.genivi.sota.messaging.Messages.{DeviceCreated, DeviceDeleted}
import org.genivi.sota.rest.Handlers.{exceptionHandler, rejectionHandler}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.Try
import slick.driver.MySQLDriver.api._

/**
 * Base API routing class.
 * @see {@linktourl http://advancedtelematic.github.io/rvi_sota_server/dev/api.html}
 */
class Routing(namespaceExtractor: Directive1[Namespace], messageBusPublisherCreated: MessageBusPublisher[DeviceCreated],
              messageBusPublisherDeleted: MessageBusPublisher[DeviceDeleted])
  (implicit db: Database, system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext)
    extends Directives {

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        new Routes(namespaceExtractor, messageBusPublisherCreated, messageBusPublisherDeleted).route
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

  lazy val messageBusPublishDeviceCreated: MessageBusPublisher[DeviceCreated] =
    MessageBusManager.getPublisher[DeviceCreated](system, config) match {
      case Xor.Right(v) => v
      case Xor.Left(error) =>
        log.error("Could not initialize message bus publisher", error)
        MessageBusPublisher.ignore
    }

  lazy val messageBusPublishDeviceDeleted: MessageBusPublisher[DeviceDeleted] =
    MessageBusManager.getPublisher[DeviceDeleted](system, config) match {
      case Xor.Right(v) => v
      case Xor.Left(error) =>
        log.error("Could not initialize message bus publisher", error)
        MessageBusPublisher.ignore
    }

  val routes: Route =
    (logResponseMetrics("device-registry") & versionHeaders(version)) {
      (handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)) {
        pathPrefix("api" / "v1") {
          new Routes(authNamespace, messageBusPublishDeviceCreated, messageBusPublishDeviceDeleted).route
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
