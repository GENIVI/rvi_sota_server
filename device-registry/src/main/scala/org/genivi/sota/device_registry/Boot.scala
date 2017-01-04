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
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.db.{BootMigrations, DatabaseConfig}
import org.genivi.sota.device_registry.db.DeviceRepository
import org.genivi.sota.http.UuidDirectives.{allowExtractor, extractUuid}
import org.genivi.sota.http._
import org.genivi.sota.messaging.{MessageBus, MessageBusPublisher}
import org.genivi.sota.monitoring.{DatabaseMetrics, MetricsSupport}
import org.genivi.sota.rest.SotaRejectionHandler.rejectionHandler
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * Base API routing class.
 * @see {@linktourl http://advancedtelematic.github.io/rvi_sota_server/dev/api.html}
 */
class DeviceRegistryRoutes(namespaceExtractor: Directive1[AuthedNamespaceScope],
                           deviceNamespaceAuthorizer: Directive1[Uuid], messageBus: MessageBusPublisher)
                          (implicit db: Database, system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext)
    extends Directives {

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      ErrorHandler.handleErrors {
        new DevicesResource(namespaceExtractor, messageBus, deviceNamespaceAuthorizer).route ~
        new SystemInfoResource(namespaceExtractor, deviceNamespaceAuthorizer).route ~
        new GroupsResource(namespaceExtractor, deviceNamespaceAuthorizer).route
      }
    }
  }
}

object Boot extends BootApp with Directives with BootMigrations
  with VersionInfo
  with DatabaseConfig
  with MetricsSupport
  with DatabaseMetrics {

  import LogDirectives._
  import VersionDirectives._

  implicit val _db = db
  lazy val config = system.settings.config

  val authNamespace = NamespaceDirectives.fromConfig()

  private val namespaceAuthorizer = allowExtractor(authNamespace, extractUuid, deviceAllowed)

  private def deviceAllowed(deviceId: Uuid): Future[Namespace] = {
    db.run(DeviceRepository.deviceNamespace(deviceId))
  }

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
      new DeviceRegistryRoutes(authNamespace, namespaceAuthorizer, messageBus).route ~
      new HealthResource(db, versionMap).route
    }

  val host = config.getString("server.host")
  val port = config.getInt("server.port")
  Http().bindAndHandle(routes, host, port)

  log.info(s"device registry started at http://$host:$port/")

  sys.addShutdownHook {
    Try(db.close())
    Try(system.terminate())
  }
}
