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
import com.advancedtelematic.libtuf.reposerver.{ReposerverClient, ReposerverHttpClient}
import com.typesafe.config.ConfigFactory
import org.genivi.sota.client.DeviceRegistryClient
import org.genivi.sota.core.daemon.{DeltaListener, TreehubCommitListener}
import org.genivi.sota.core.resolver._
import org.genivi.sota.core.rvi._
import org.genivi.sota.core.storage.S3PackageStore
import org.genivi.sota.core.transfer._
import org.genivi.sota.core.user_profile._
import org.genivi.sota.db.{BootMigrations, DatabaseConfig}
import org.genivi.sota.http.LogDirectives._
import org.genivi.sota.http._
import org.genivi.sota.messaging.Messages.{DeltaGenerationFailed, GeneratedDelta, TreehubCommit}
import org.genivi.sota.messaging.daemon.MessageBusListenerActor.Subscribe
import org.genivi.sota.messaging.kafka.MessageListener
import org.genivi.sota.messaging.{MessageBus, MessageBusPublisher}
import org.genivi.sota.monitoring.{DatabaseMetrics, MetricsSupport}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import slick.jdbc.MySQLProfile.api._

trait RviBoot {
  self: Settings =>

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val exec: ExecutionContext
  implicit lazy val rviConnectivity = new RviConnectivity(rviEndpoint)

  def resolverClient: ExternalResolverClient

  def deviceRegistryClient: DeviceRegistryClient

  def messageBusPublisher: MessageBusPublisher

  def startSotaServices(db: Database): Route = {
    val s3PackageStoreOpt = S3PackageStore.loadCredentials(config).map { new S3PackageStore(_) }
    val transferProtocolProps =
      TransferProtocolActor.props(db, rviConnectivity.client,
        PackageTransferActor.props(rviConnectivity.client, s3PackageStoreOpt), messageBusPublisher)
    val updateController = system.actorOf(UpdateController.props(transferProtocolProps ), "update-controller")
    new rvi.SotaServices(updateController, resolverClient).route
  }

  def rviRoutes(db: Database, notifier: UpdateNotifier, namespaceDirective: Directive1[AuthedNamespaceScope]): Route = {
    val updateService = new UpdateService(notifier, deviceRegistryClient)

    new WebService(updateService, resolverClient,
      deviceRegistryClient, db, namespaceDirective, messageBusPublisher).route ~
    startSotaServices(db)
  }

  def rviInteraction(db: Database,
                     namespaceDirective: Directive1[AuthedNamespaceScope]): Future[(Route, UpdateNotifier)] = {
    SotaServices.register(rviSotaUri).map { sotaServices =>
      val updateNotifier = new RviUpdateNotifier(sotaServices)
      (rviRoutes(db, updateNotifier, namespaceDirective), updateNotifier)
    }
  }
}

trait HttpBoot {
  self: Settings =>

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
                            namespaceDirective: Directive1[AuthedNamespaceScope]): (Route, UpdateNotifier) = {

    val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistryClient)
    val webService = new WebService(updateService, resolverClient, deviceRegistryClient, db,
      namespaceDirective, messageBusPublisher)

    val deviceService = new DeviceUpdatesResource(db, resolverClient, deviceRegistryClient,
      userProfileClient, namespaceDirective, messageBusPublisher)

    val routes = tokenValidator {
      webService.route ~ deviceService.route
    }

    (routes, DefaultUpdateNotifier)
  }
}

// TODO: Should be moved to separate process
trait AsyncListeners {
  self: Settings ⇒

  implicit val system: ActorSystem
  implicit val exec: ExecutionContext
  implicit val defaultConnectivity: Connectivity
  implicit val materializer: ActorMaterializer

  def deviceRegistryClient: DeviceRegistryClient

  def messageBusPublisher: MessageBusPublisher

  def startTreehubCommitListener(db: Database, notifier: UpdateNotifier, tufClient: ReposerverClient): Unit = {
    val updateService = new UpdateService(notifier, deviceRegistryClient)

    val listenerFn = new TreehubCommitListener(db, updateService, tufClient, messageBusPublisher).action(_)
    val listener = system.actorOf(MessageListener.props[TreehubCommit](config, listenerFn))

    listener ! Subscribe
  }

  def startDeltaListeners(db: Database, notifier: UpdateNotifier): Unit = {
    val updateService = new UpdateService(notifier, deviceRegistryClient)

    val deltaListener = new DeltaListener(deviceRegistryClient, updateService, messageBusPublisher)(db)

    val generatedDeltaListener = system.actorOf(MessageListener.props[GeneratedDelta](config,
      deltaListener.generatedDeltaAction))
    generatedDeltaListener ! Subscribe

    val failedListener = system.actorOf(MessageListener.props[DeltaGenerationFailed](config,
      deltaListener.deltaGenerationFailedAction))
    failedListener ! Subscribe
  }
}

trait Settings {

  lazy val config = ConfigFactory.load()

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
  val deviceRegistryPackagesApi = Uri(config.getString("device_registry.packagesUri"))

  val userProfileBaseUri =
    if (config.getBoolean("user_profile.use")) Some(Uri(config.getString("user_profile.baseUri")))
    else None
  val userProfileApi =
    if (config.getBoolean("user_profile.use")) Some(Uri(config.getString("user_profile.api")))
    else None

  val rviSotaUri = Uri(config.getString("rvi.sotaServicesUri"))
  val rviEndpoint = Uri(config.getString("rvi.endpoint"))

  val tufEndpoint =
    if (config.getBoolean("tuf.use")) Some(Uri(config.getString("tuf.uri")))
    else None

  val startAsyncListeners = Try(config.getBoolean("core.startAsyncListeners")).getOrElse(true)
}

object Settings extends Settings


object Boot extends BootApp
  with Settings
  with VersionInfo
  with DatabaseConfig
  with HttpBoot
  with RviBoot
  with AsyncListeners
  with BootMigrations
  with MetricsSupport
  with DatabaseMetrics {

  import VersionDirectives._

  val resolverClient = new DefaultExternalResolverClient(
    resolverUri, resolverResolveUri, resolverPackagesUri, resolverVehiclesUri
  )

  val deviceRegistryClient = new DeviceRegistryClient(
    deviceRegistryUri, deviceRegistryApi,
    deviceRegistryGroupApi, deviceRegistryMyApi,
    deviceRegistryPackagesApi
  )

  val userProfileClient = for {
    baseUri <- userProfileBaseUri
    api <- userProfileApi
  } yield new UserProfileClient(baseUri, api)

  val messageBusPublisher: MessageBusPublisher =
    MessageBus.publisher(system, config) match {
      case Right(c) => c
      case Left(err) =>
        log.error("Could not initialize message bus client", err)
        MessageBusPublisher.ignore
    }

  val tufClient = tufEndpoint.map(uri => new ReposerverHttpClient(uri))

  val interactionProtocol = config.getString("core.interactionProtocol")

  val healthResource = new HealthResource(db, versionMap)

  log.info(s"using interaction protocol '$interactionProtocol'")

  val sotaLog: Directive0 = {
    logRequestResult(("sota-core", Logging.DebugLevel)) &
      TraceId.withTraceId &
      logResponseMetrics("sota-core", TraceId.traceMetrics) &
      versionHeaders(version)
  }

  def routes(): Future[(Route, UpdateNotifier)] = interactionProtocol match {
    case "rvi" =>
      rviInteraction(db, NamespaceDirectives.fromConfig())

    case _ =>
      FastFuture.successful {
        httpInteractionRoutes(db, TokenValidator().fromConfig(), NamespaceDirectives.fromConfig())
      }
  }

  val binding =
    routes().flatMap { case (routes, updateNotifier) =>
      val fullRoutes = routes ~ healthResource.route
      val sealedRoutes = sotaLog(Route.seal(fullRoutes))

      if (startAsyncListeners) {
        if (tufClient.isDefined)
          startTreehubCommitListener(db, updateNotifier, tufClient.get)

        startDeltaListeners(db, updateNotifier)
      }

      Http().bindAndHandle(sealedRoutes, host, port)
    }

  binding onComplete {
    case Success(_) =>
      log.info(s"Server online at http://$host:$port")
    case Failure(e) =>
      log.error("Unable to start", e)
      sys.exit(-1)
  }

  sys.addShutdownHook {
    Try(db.close())
    Try(system.terminate())
  }
}

