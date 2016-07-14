/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Directive1, Route}
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import org.genivi.sota.client.DeviceRegistryClient
import org.genivi.sota.core.db.DatabaseConfig
import org.genivi.sota.core.resolver._
import org.genivi.sota.core.rvi._
import org.genivi.sota.core.storage.S3PackageStore
import org.genivi.sota.core.transfer._
import org.genivi.sota.data.Namespace
import org.genivi.sota.http.AuthDirectives.AuthScope
import org.genivi.sota.http._
import org.genivi.sota.http.LogDirectives._
import slick.driver.MySQLDriver.api.Database

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


trait RviBoot {
  val settings: Settings

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val exec: ExecutionContext
  implicit lazy val rviConnectivity = new RviConnectivity(settings.rviEndpoint)

  def resolverClient: ExternalResolverClient

  def deviceRegistryClient: DeviceRegistryClient

  def startSotaServices(db: Database): Route = {
    val s3PackageStoreOpt = S3PackageStore.loadCredentials(settings.config).map { new S3PackageStore(_) }
    val transferProtocolProps =
      TransferProtocolActor.props(db, rviConnectivity.client,
        PackageTransferActor.props(rviConnectivity.client, s3PackageStoreOpt))
    val updateController = system.actorOf(UpdateController.props(transferProtocolProps ), "update-controller")
    new rvi.SotaServices(updateController, resolverClient, deviceRegistryClient).route
  }

  def rviRoutes(db: Database, notifier: UpdateNotifier, namespaceDirective: Directive1[Namespace]): Route = {
      new WebService(notifier, resolverClient, deviceRegistryClient, db, namespaceDirective).route ~
      startSotaServices(db)
  }

  def rviInteractionRoutes(db: Database, namespaceDirective: Directive1[Namespace]): Future[Route] = {
    SotaServices.register(settings.rviSotaUri) map { sotaServices =>
      rviRoutes(db, new RviUpdateNotifier(sotaServices), namespaceDirective)
    }
  }
}

trait HttpBoot {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val defaultConnectivity: Connectivity = DefaultConnectivity
  implicit val exec: ExecutionContext

  def resolverClient: ExternalResolverClient

  def deviceRegistryClient: DeviceRegistryClient

  def httpInteractionRoutes(db: Database,
                            namespaceDirective: Directive1[Namespace],
                            authDirective: AuthScope => Directive0
                           ): Route = {
    val webService = new WebService(DefaultUpdateNotifier, resolverClient, deviceRegistryClient, db,
      namespaceDirective)
    val vehicleService = new DeviceUpdatesResource(db, resolverClient, deviceRegistryClient,
      namespaceDirective, authDirective)

    webService.route ~ vehicleService.route
  }
}


class Settings(val config: Config) {
  val host = config.getString("server.host")
  val port = config.getInt("server.port")

  val resolverUri = Uri(config.getString("resolver.baseUri"))
  val resolverResolveUri = Uri(config.getString("resolver.resolveUri"))
  val resolverPackagesUri = Uri(config.getString("resolver.packagesUri"))
  val resolverVehiclesUri = Uri(config.getString("resolver.vehiclesUri"))

  val deviceRegistryUri = Uri(config.getString("device_registry.baseUri"))
  val deviceRegistryApi = Uri(config.getString("device_registry.devicesUri"))

  val rviSotaUri = Uri(config.getString("rvi.sotaServicesUri"))
  val rviEndpoint = Uri(config.getString("rvi.endpoint"))
}


object Boot extends App with DatabaseConfig with HttpBoot with RviBoot {
  import VersionDirectives._

  implicit val system = ActorSystem("sota-core-service")
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher
  implicit val log = Logging(system, "boot")
  val config = system.settings.config
  val settings = new Settings(config)

  lazy val version: String = {
    val bi = org.genivi.sota.core.BuildInfo
    bi.name + "/" + bi.version
  }

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

  val resolverClient = new DefaultExternalResolverClient(
    settings.resolverUri, settings.resolverResolveUri, settings.resolverPackagesUri, settings.resolverVehiclesUri
  )

  val deviceRegistryClient = new DeviceRegistryClient(
    settings.deviceRegistryUri, settings.deviceRegistryApi
  )
  
  val interactionProtocol = config.getString("core.interactionProtocol")

  val healthResource = new HealthResource(db, org.genivi.sota.core.BuildInfo.toMap)

  log.info(s"using interaction protocol '$interactionProtocol'")

  val sotaLog: Directive0 = {
    logRequestResult(("sota-core", Logging.DebugLevel)) &
      TraceId.withTraceId &
      logResponseMetrics("sota-core", TraceId.traceMetrics) &
      versionHeaders(version)
  }

  def routes(): Future[Route] = interactionProtocol match {
    case "rvi" =>
      rviInteractionRoutes(db, NamespaceDirectives.fromConfig()).map(_ ~ healthResource.route)

    case _ =>
      FastFuture.successful {
        httpInteractionRoutes(db, NamespaceDirectives.fromConfig(), AuthDirectives.fromConfig()) ~
          healthResource.route
      }
  }

  val startupF =
    routes() map { r =>
      val sealedRoutes = sotaLog(Route.seal(r))

      Http()
        .bindAndHandle(sealedRoutes, settings.host, settings.port)
    }

  startupF onComplete {
    case Success(services) =>
      log.info(s"Server online at http://${settings.host}:${settings.port}")
    case Failure(e) =>
      log.error(e, "Unable to start")
      sys.exit(-1)
  }

  sys.addShutdownHook {
    Try(db.close())
    Try(system.terminate())
  }
}

