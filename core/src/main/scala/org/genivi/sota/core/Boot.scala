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
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import org.genivi.sota.client.DeviceRegistryClient
import org.genivi.sota.core.db.DatabaseConfig
import org.genivi.sota.core.resolver.{Connectivity, DefaultConnectivity, DefaultExternalResolverClient}
import org.genivi.sota.core.rvi._
import org.genivi.sota.core.storage.S3PackageStore
import org.genivi.sota.core.transfer._
import org.genivi.sota.http._
import org.genivi.sota.http.LogDirectives._

import scala.util.{Failure, Success, Try}


class Settings(config: Config) {
  val host = config.getString("server.host")
  val port = config.getInt("server.port")

  val resolverUri = Uri(config.getString("resolver.baseUri"))
  val resolverResolveUri = Uri(config.getString("resolver.resolveUri"))
  val resolverPackagesUri = Uri(config.getString("resolver.packagesUri"))
  val resolverVehiclesUri = Uri(config.getString("resolver.vehiclesUri"))

  val deviceRegistryUri = Uri(config.getString("device_registry.baseUri"))
  val deviceRegistryApi = Uri(config.getString("device_registry.devicesUri"))
}


object Boot extends App with DatabaseConfig {
  import slick.driver.MySQLDriver.api.Database
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

  val externalResolverClient = new DefaultExternalResolverClient(
    settings.resolverUri, settings.resolverResolveUri, settings.resolverPackagesUri, settings.resolverVehiclesUri
  )

  val deviceRegistry= new DeviceRegistryClient(
    settings.deviceRegistryUri, settings.deviceRegistryApi
  )
  
  val interactionProtocol = config.getString("core.interactionProtocol")

  log.info(s"using interaction protocol '$interactionProtocol'")

  def startSotaServices(db: Database): Route = {
    val s3PackageStoreOpt = S3PackageStore.loadCredentials(config).map { new S3PackageStore(_) }
    val transferProtocolProps =
      TransferProtocolActor.props(db, connectivity.client,
        PackageTransferActor.props(connectivity.client, s3PackageStoreOpt))
    val updateController = system.actorOf(UpdateController.props(transferProtocolProps ), "update-controller")
    new rvi.SotaServices(updateController, externalResolverClient, deviceRegistry).route
  }

  def rviRoutes(notifier: UpdateNotifier): Route = {
    new HealthResource(db, org.genivi.sota.core.BuildInfo.toMap).route ~
      new WebService(notifier, externalResolverClient, deviceRegistry, db, NamespaceDirectives.fromConfig()).route ~
      startSotaServices(db)
  }

  implicit val connectivity: Connectivity = interactionProtocol match {
    case "rvi" => new RviConnectivity
    case _ => DefaultConnectivity
  }

  val healthResource = new HealthResource(db, org.genivi.sota.core.BuildInfo.toMap)
  val webService = new WebService(DefaultUpdateNotifier, externalResolverClient, deviceRegistry, db,
    NamespaceDirectives.fromConfig())
  val vehicleService = new DeviceUpdatesResource(db,
     externalResolverClient, deviceRegistry, NamespaceDirectives.fromConfig(), AuthDirectives.fromConfig())

  val routes = Route.seal(
    healthResource.route ~
      webService.route ~
      vehicleService.route
  )

  val loggedRoutes = {
    (logRequestResult(("sota-core", Logging.DebugLevel)) &
      TraceId.withTraceId &
      logResponseMetrics("sota-core", TraceId.traceMetrics) &
      versionHeaders(version)) {
      routes
    }
  }

  val startup = interactionProtocol match {
    case "rvi" => for {
      sotaServices <- SotaServices.register(Uri(config.getString("rvi.sotaServicesUri")))
      notifier      = new RviUpdateNotifier(sotaServices)
      binding      <- Http().bindAndHandle(rviRoutes(notifier), settings.host, settings.port)
    } yield sotaServices
    case _ =>

      Http()
        .bindAndHandle(loggedRoutes, settings.host, settings.port)
        .map(_ => ServerServices("","","",""))
  }

  startup onComplete {
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

