/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.genivi.sota.core.db._
import org.genivi.sota.core.resolver.{Connectivity, DefaultConnectivity, DefaultExternalResolverClient}
import org.genivi.sota.core.rvi._
import org.genivi.sota.core.transfer._
import org.genivi.sota.data.Namespace._
import org.genivi.sota.device_registry.client.DeviceRegistryClient
import org.genivi.sota.http.SotaDirectives._
import scala.util.{Failure, Success, Try}


object Boot extends App with DatabaseConfig {

  import slick.driver.MySQLDriver.api.Database
  def startSotaServices(db: Database): Route = {
    val transferProtocolProps =
      TransferProtocolActor.props(db, connectivity.client,
                                  PackageTransferActor.props(connectivity.client))
    val updateController = system.actorOf( UpdateController.props(transferProtocolProps ), "update-controller")
    new rvi.SotaServices(updateController, externalResolverClient).route
  }

  implicit val system = ActorSystem("sota-core-service")
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher
  implicit val log = Logging(system, "boot")
  val config = system.settings.config

  lazy val version = {
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
    Uri(config.getString("resolver.baseUri")),
    Uri(config.getString("resolver.resolveUri")),
    Uri(config.getString("resolver.packagesUri")),
    Uri(config.getString("resolver.vehiclesUri"))
  )

  val deviceRegistryClient = new DeviceRegistryClient(
    Uri(config.getString("device_registry.baseUri")),
    Uri(config.getString("device_registry.devicesUri"))
  )


  val host = config.getString("server.host")
  val port = config.getInt("server.port")
  val interactionProtocol = config.getString("core.interactionProtocol")
  log.info(s"using interaction protocol '$interactionProtocol'")

  import Directives._
  import org.genivi.sota.core.rvi.ServerServices

  def routes(notifier: UpdateNotifier): Route = {
    new WebService(notifier, externalResolverClient, deviceRegistryClient, db).route ~ startSotaServices(db)
  }

  implicit val connectivity: Connectivity = interactionProtocol match {
    case "rvi" => new RviConnectivity
    case _ => DefaultConnectivity
  }

  val startup = interactionProtocol match {
    case "rvi" => for {
      sotaServices <- SotaServices.register(Uri(config.getString("rvi.sotaServicesUri")))
      notifier      = new RviUpdateNotifier(sotaServices)
      binding      <- Http().bindAndHandle(routes(notifier), host, port)
    } yield sotaServices
    case _ =>
      val notifier = DefaultUpdateNotifier
      val vehicleService = new VehicleUpdatesResource(db, externalResolverClient, deviceRegistryClient)
      val allRoutes = Route.seal(routes(notifier) ~ vehicleService.route)
      val versionRoutes = (logResponseMetrics("sota-core") & versionHeaders(version))(allRoutes)

      Http()
        .bindAndHandle(versionRoutes, host, port)
        .map(_ => ServerServices("","","",""))
  }

  startup.onComplete {
    case Success(services) =>
      log.info(s"Server online at http://$host:$port")
    case Failure(e) =>
      log.error(e, "Unable to start")
      sys.exit(-1)
  }

  sys.addShutdownHook {
    Try( db.close()  )
    Try( system.terminate() )
  }
}
