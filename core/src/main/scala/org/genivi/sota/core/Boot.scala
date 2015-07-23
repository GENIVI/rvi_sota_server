/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.{ActorLogging, ActorSystem}
import akka.event.LoggingAdapter
import akka.event.{BusLogging, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer
import db._
import scala.concurrent.{ExecutionContext, Future}
import spray.json.{JsObject, JsString}

trait Protocols extends DateTimeJsonProtocol {
  implicit val installCampaignFormat = jsonFormat5(InstallCampaign.apply)
}

class WebService(implicit system: ActorSystem,
                 mat: ActorMaterializer,
                 exec: ExecutionContext,
                 log: LoggingAdapter) extends Directives with Protocols {

  val resolverHost = system.settings.config.getString("resolver.host")
  val resolverPort = system.settings.config.getInt("resolver.port")
  val resolver = new Resolver(resolverHost, resolverPort)

  val exceptionHandler = ExceptionHandler {
    case e: Throwable =>
      extractUri { uri =>
        log.error(s"Request to $uri errored: $e")
        val entity = JsObject("error" -> JsString(e.getMessage()))
        complete(HttpResponse(InternalServerError, entity = entity.toString()))
      }
  }

  val route = pathPrefix("api" / "v1") {
    handleExceptions(exceptionHandler) {
      path("install_campaigns") {
        (post & entity(as[InstallCampaign])) { campaign =>
          complete {
            for {
              dependencyMap     <- resolver.resolve(campaign.packageId)
              persistedCampaign <- InstallCampaigns.create(campaign)
              _   <- InstallRequests.create(
                InstallRequest.from(dependencyMap, persistedCampaign.id.head).toSeq
              )
            } yield persistedCampaign
          }
        }
      }
    }
  }
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
