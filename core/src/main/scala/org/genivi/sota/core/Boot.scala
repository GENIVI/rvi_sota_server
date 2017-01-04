/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
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
import cats.data.Xor
import com.typesafe.config.Config
import org.genivi.sota.client.DeviceRegistryClient
import org.genivi.sota.core.resolver._
import org.genivi.sota.core.rvi._
import org.genivi.sota.core.storage.S3PackageStore
import org.genivi.sota.core.transfer._
import org.genivi.sota.core.user_profile._
import org.genivi.sota.db.{BootMigrations, DatabaseConfig}
import org.genivi.sota.http._
import org.genivi.sota.http.LogDirectives._
import org.genivi.sota.messaging.{MessageBus, MessageBusPublisher}
import org.genivi.sota.monitoring.{DatabaseMetrics, MetricsSupport}
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

  def messageBusPublisher: MessageBusPublisher

  def startSotaServices(db: Database): Route = {
    val s3PackageStoreOpt = S3PackageStore.loadCredentials(settings.config).map { new S3PackageStore(_) }
    val transferProtocolProps =
      TransferProtocolActor.props(db, rviConnectivity.client,
        PackageTransferActor.props(rviConnectivity.client, s3PackageStoreOpt), messageBusPublisher)
    val updateController = system.actorOf(UpdateController.props(transferProtocolProps ), "update-controller")
    new rvi.SotaServices(updateController, resolverClient).route
  }

  def rviRoutes(db: Database, notifier: UpdateNotifier, namespaceDirective: Directive1[AuthedNamespaceScope]): Route = {
      new WebService(notifier, resolverClient,
        deviceRegistryClient, db, namespaceDirective, messageBusPublisher).route ~
      startSotaServices(db)
  }

  def rviInteractionRoutes(db: Database, namespaceDirective: Directive1[AuthedNamespaceScope]): Future[Route] = {
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

  def userProfileClient: Option[UserProfileClient]

  def messageBusPublisher: MessageBusPublisher

  def httpInteractionRoutes(db: Database,
                            tokenValidator: Directive0,
                            namespaceDirective: Directive1[AuthedNamespaceScope],
                            messageBus: MessageBusPublisher): Route = {
    val webService = new WebService(DefaultUpdateNotifier, resolverClient, deviceRegistryClient, db,
      namespaceDirective, messageBusPublisher)
    val deviceService = new DeviceUpdatesResource(db, resolverClient, deviceRegistryClient,
      userProfileClient, namespaceDirective, messageBus)

    tokenValidator {
      webService.route ~ deviceService.route
    }
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
  val deviceRegistryGroupApi = Uri(config.getString("device_registry.deviceGroupsUri"))
  val deviceRegistryMyApi = Uri(config.getString("device_registry.mydeviceUri"))

  val userProfileBaseUri =
    if (config.getBoolean("user_profile.use")) Some(Uri(config.getString("user_profile.baseUri")))
    else None
  val userProfileApi =
    if (config.getBoolean("user_profile.use")) Some(Uri(config.getString("user_profile.api")))
    else None

  val rviSotaUri = Uri(config.getString("rvi.sotaServicesUri"))
  val rviEndpoint = Uri(config.getString("rvi.endpoint"))
}


object Boot extends BootApp
  with VersionInfo
  with DatabaseConfig
  with HttpBoot
  with RviBoot
  with BootMigrations
  with MetricsSupport
  with DatabaseMetrics {

  import VersionDirectives._

  lazy val config = system.settings.config
  val settings = new Settings(config)

  val resolverClient = new DefaultExternalResolverClient(
    settings.resolverUri, settings.resolverResolveUri, settings.resolverPackagesUri, settings.resolverVehiclesUri
  )

  val deviceRegistryClient = new DeviceRegistryClient(
    settings.deviceRegistryUri, settings.deviceRegistryApi,
    settings.deviceRegistryGroupApi, settings.deviceRegistryMyApi
  )

  val userProfileClient = for {
    baseUri <- settings.userProfileBaseUri
    api <- settings.userProfileApi
  } yield new UserProfileClient(baseUri, api)

  val messageBusPublisher: MessageBusPublisher =
    MessageBus.publisher(system, system.settings.config) match {
      case Xor.Right(c) => c
      case Xor.Left(err) =>
        log.error("Could not initialize message bus client", err)
        MessageBusPublisher.ignore
    }

  val interactionProtocol = config.getString("core.interactionProtocol")

  val healthResource = new HealthResource(db, versionMap)

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
        httpInteractionRoutes(db, TokenValidator().fromConfig(), NamespaceDirectives.fromConfig(),
                              messageBusPublisher) ~
          healthResource.route
      }
  }

  val binding =
    routes().flatMap { r =>
      val sealedRoutes = sotaLog(Route.seal(r))

      Http()
        .bindAndHandle(sealedRoutes, settings.host, settings.port)
    }

  binding onComplete {
    case Success(services) =>
      log.info(s"Server online at http://${settings.host}:${settings.port}")
    case Failure(e) =>
      log.error("Unable to start", e)
      sys.exit(-1)
  }

  sys.addShutdownHook {
    Try(db.close())
    Try(system.terminate())
  }
}

