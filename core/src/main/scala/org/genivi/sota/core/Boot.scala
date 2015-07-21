/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.ActorMaterializer
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import db._

trait Protocols extends DateTimeJsonProtocol {
  implicit val installRequestFormat = jsonFormat5(InstallRequest.apply)
}

class WebService(implicit system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext) extends Directives with Protocols {

  val route = reject

}

object Boot extends App {
  implicit val system = ActorSystem("sota-core-service")
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher
  implicit val log = Logging(system, "boot")

  val service = new WebService()

  val host = system.settings.config.getString("server.host")
  val port = system.settings.config.getInt("server.port")

  val bindingFuture = Http().bindAndHandle(service.route, host, port)

  log.info(s"Server online at http://$host:$port/\nPress RETURN to stop...")
}
