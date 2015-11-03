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
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import org.genivi.sota.core.db._
import org.genivi.sota.core.jsonrpc.HttpTransport
import org.genivi.sota.core.rvi._

import scala.util.{Failure, Success, Try}

object Boot extends App with DatabaseConfig {

  import slick.driver.MySQLDriver.api.Database
  def startSotaServices(db: Database) : Route = {
    val transferProtocolProps = TransferProtocolActor.props(db, rviClient, PackageTransferActor.props( rviClient ))
    val updateController = system.actorOf( UpdateController.props(transferProtocolProps ), "update-controller")
    new rvi.SotaServices(updateController, externalResolverClient).route
  }

  implicit val system = ActorSystem("sota-core-service")
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher
  implicit val log = Logging(system, "boot")
  val config = system.settings.config

  val externalResolverClient = new DefaultExternalResolverClient(
    Uri(config.getString("resolver.baseUri")),
    Uri(config.getString("resolver.resolveUri")),
    Uri(config.getString("resolver.packagesUri")),
    Uri(config.getString("resolver.vehiclesUri"))
  )

  val host = config.getString("server.host")
  val port = config.getInt("server.port")

  import Directives._
  import org.genivi.sota.core.rvi.ServerServices
  def routes(rviServices: ServerServices) = {
    new WebService( rviServices, externalResolverClient, db ).route ~ startSotaServices(db)
  }

  val rviUri = Uri(system.settings.config.getString( "rvi.endpoint" ))
  implicit val requestTransport = HttpTransport(rviUri).requestTransport
  implicit val rviClient = new JsonRpcRviClient( requestTransport, system.dispatcher )

  val sotaServicesFuture = for {
    sotaServices <- SotaServices.register( Uri(config.getString("rvi.sotaServicesUri")) )
    binding      <- Http().bindAndHandle(routes(sotaServices), host, port)
  } yield sotaServices

  sotaServicesFuture.onComplete {
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
