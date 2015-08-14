/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, ExceptionHandler, PathMatchers}
import akka.stream.ActorMaterializer
import java.nio.file.Paths
import org.genivi.sota.core.data._
import org.genivi.sota.core.db._
import org.genivi.sota.core.files.Types.ValidExtension
import org.genivi.sota.core.rvi._
import org.genivi.sota.rest.Validation._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import slick.driver.MySQLDriver.api.Database
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsString}


class WebService(db : Database)(implicit system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext) extends Directives {

  val resolverHost = system.settings.config.getString("resolver.host")
  val resolverPort = system.settings.config.getInt("resolver.port")
  val resolver = new DependencyResolver(resolverHost, resolverPort)
  val log = Logging(system, "webservice")

  val exceptionHandler = ExceptionHandler {
    case e: Throwable =>
      extractUri { uri =>
        log.error(s"Request to $uri errored: $e")
        val entity = JsObject("error" -> JsString(e.getMessage()))
        complete(HttpResponse(InternalServerError, entity = entity.toString()))
      }
  }

  def createCampaign( campaign : InstallCampaign ) : Future[InstallCampaign] = {
    def persistCampaign( dependencies : Map[Vehicle, Set[PackageId]] ) = for {
      persistedCampaign <- InstallCampaigns.create(campaign)
      _   <- InstallRequests.createRequests( InstallRequest.from(dependencies, persistedCampaign.id.head).toSeq )
    } yield persistedCampaign

    for {
      dependencyMap     <- resolver.resolve(campaign.packageId)
      persistedCampaign <- db.run(persistCampaign( dependencyMap ))
    } yield persistedCampaign
  }

  val route = pathPrefix("api" / "v1") {
    handleExceptions(exceptionHandler) {
      path("install_campaigns") {
        (post & entity(as[InstallCampaign])) { campaign =>
          complete {
            createCampaign( campaign )
          }
        }
      } ~
      pathPrefix("vehicles") {
        (put & refined[Vehicle.Vin](PathMatchers.Slash ~ PathMatchers.Segment ~ PathMatchers.PathEnd)) { vin =>
          complete(db.run( Vehicles.create(Vehicle(vin)) ).map(_ => NoContent))
        }
      } ~
      path("vehicles") {
        get {
          parameters('regex.?) { (regex) =>
            val query = regex match {
              case Some(r) => Vehicles.searchByRegex(r)
              case _ => Vehicles.list()
            }
            complete(db.run(query))
          }
        }
      } ~
      path("packages") {
        get {
          complete {
            NoContent
            //Packages.list
          }
        }
      }
    }
  }
}

import eu.timepit.refined._
import eu.timepit.refined.string._

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
    path <- refineV[Uri](config.getString("packages.absolutePath")).right
    fileExt <- refineV[ValidExtension](config.getString("packages.extension")).right
    checksumExt <- refineV[ValidExtension](config.getString("packages.checksumExtension")).right
  } yield new files.Resolver(path, fileExt, checksumExt)).fold(err => throw new Exception(err), identity)

  val deviceCommunication = new DeviceCommunication(db, rviInterface, fileResolver, e => log.error(e, e.getMessage()))

  val service = new WebService( db )

  import scala.concurrent.duration._
  val cancellable = system.scheduler.schedule(50.millis, 1.second) { deviceCommunication.runCurrentCampaigns }

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
