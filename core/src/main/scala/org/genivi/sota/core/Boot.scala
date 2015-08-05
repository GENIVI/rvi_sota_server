/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.Props
import akka.actor.{ActorLogging, ActorSystem}
import akka.event.LoggingAdapter
import akka.event.{BusLogging, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer
import java.util.concurrent.TimeUnit
import org.genivi.sota.core.db._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import spray.json.{JsObject, JsString}

object JsonProtocols extends DateTimeJsonProtocol {
  implicit val installCampaignFormat = jsonFormat5(InstallCampaign.apply)
  implicit val vinFormat = jsonFormat(Vin, "vin")
  implicit val pkgFormat = jsonFormat5(Package.apply)
}

import slick.driver.MySQLDriver.api.Database
class WebService(db : Database)(implicit system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext) extends Directives {
  import JsonProtocols._

  val resolverHost = system.settings.config.getString("resolver.host")
  val resolverPort = system.settings.config.getInt("resolver.port")
  val resolver = new Resolver(resolverHost, resolverPort)
  val log = Logging(system, "webservice")

  val exceptionHandler = ExceptionHandler {
    case e: Throwable =>
      extractUri { uri =>
        log.error(s"Request to $uri errored: $e")
        val entity = JsObject("error" -> JsString(e.getMessage()))
        complete(HttpResponse(InternalServerError, entity = entity.toString()))
      }
  }

  def createCompain( campaign : InstallCampaign ) : Future[InstallCampaign] = {
    def persistCompain( dependencies : Map[Vin, Set[Long]] ) = for {
      persistedCampaign <- InstallCampaigns.create(campaign)
      _   <- InstallRequests.createRequests( InstallRequest.from(dependencies, persistedCampaign.id.head).toSeq )
    } yield persistedCampaign

    for {
      dependencyMap     <- resolver.resolve(campaign.packageId)
      persistedCampaign  <- db.run( persistCompain( dependencyMap)  ) 
    } yield persistedCampaign
  }

  val route = pathPrefix("api" / "v1") {
    handleExceptions(exceptionHandler) {
      path("install_campaigns") {
        (post & entity(as[InstallCampaign])) { campaign =>
          complete {
            createCompain( campaign )
          }
        }
      } ~
      path("vins") {
        (post & entity(as[Vin])) { vin =>
          complete(db.run( Vins.create(vin) ).map(_ => NoContent))
        }
      } ~
      path("packages") {
        get {
          complete {
            NoContent
            //Packages.list
          }
        } ~
        (post & entity(as[Package])) { newPackage =>
          complete(db.run( Packages.create(newPackage) ) )
        }
      }
    }
  }
}

object Boot extends App with DatabaseConfig {
  implicit val system = ActorSystem("sota-core-service")
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher
  implicit val log = Logging(system, "boot")
  val config = system.settings.config

  val service = new WebService( db )

  val rviHost = config.getString("rvi.host")
  val rviPort = config.getInt("rvi.port")
  val rviActor = system.actorOf(RviActor.props(rviHost, rviPort, db))

  import scala.concurrent.duration._
  val cancellable = system.scheduler.schedule(50.millis, 1.second, rviActor, RviActor.Trigger)

  val host = config.getString("server.host")
  val port = config.getInt("server.port")

  val bindingFuture = Http().bindAndHandle(service.route, host, port)

  log.info(s"Server online at http://$host:$port")

  sys.addShutdownHook {
    Try( db.close()  )
    Try( system.shutdown() )
  }
}
