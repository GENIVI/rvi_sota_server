/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse}
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.stream.ActorMaterializer
import slick.driver.MySQLDriver.api.Database


class VehicleService(db : Database)
                    (implicit system: ActorSystem, mat: ActorMaterializer,
                     connectivity: Connectivity) extends Directives {
  implicit val log = Logging(system, "vehicleservice")

  import io.circe.Json
  import Json.{obj, string}

  val exceptionHandler = ExceptionHandler {
    case e: Throwable =>
      extractUri { uri =>
        log.error(s"Request to $uri errored: $e")
        val entity = obj("error" -> string(e.getMessage()))
        complete(HttpResponse(InternalServerError, entity = entity.toString()))
      }
  }
  val vehicles = new VehiclesResource(db, connectivity.client)

  val route = pathPrefix("api" / "v1") {
    handleExceptions(exceptionHandler) {
       vehicles.route
    }
  }

}
