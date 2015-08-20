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
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.string.{Uri => RUri}
import org.genivi.sota.core.db._
import org.genivi.sota.core.files.Types.ValidExtension
import org.genivi.sota.core.rvi._

import scala.util.Try

object Boot extends App with DatabaseConfig {
  implicit val system = ActorSystem("sota-core-service")
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher
  implicit val log = Logging(system, "boot")
  val config = system.settings.config

  val rviHost = config.getString("rvi.host")
  val rviPort = config.getInt("rvi.port")
  val rviInterface = new JsonRpcRviInterface(rviHost, rviPort)
  val fileResolver = (for {
    path <- refineV[RUri](config.getString("packages.absolutePath")).right
    fileExt <- refineV[ValidExtension](config.getString("packages.extension")).right
    checksumExt <- refineV[ValidExtension](config.getString("packages.checksumExtension")).right
  } yield new files.Resolver(path, fileExt, checksumExt)).fold(err => throw new Exception(err), identity)

  val deviceCommunication = new DeviceCommunication(db, rviInterface, fileResolver, e => log.error(e, e.getMessage()))
  val externalResolverClient = new DefaultExternalResolverClient( Uri(config.getString("resolver.baseUri")) )

  val service = new WebService( externalResolverClient, db )

  import scala.concurrent.duration._
  val cancellable = system.scheduler.schedule(50.millis, 1.second) { deviceCommunication.runCurrentCampaigns() }

  val host = config.getString("server.host")
  val port = config.getInt("server.port")

  import Directives._
  val routes = service.route ~ new rvi.WebService().route(deviceCommunication)

  val bindingFuture = Http().bindAndHandle(routes, host, port)

  log.info(s"Server online at http://$host:$port")

  sys.addShutdownHook {
    Try( db.close()  )
    Try( system.shutdown() )
  }
}
