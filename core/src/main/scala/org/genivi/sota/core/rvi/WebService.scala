/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core.rvi

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

class WebService(implicit mat: ActorMaterializer, exec: ExecutionContext) extends Directives {

  def route(deviceCommunication: DeviceCommunication): Route = pathPrefix("rvi") {
    complete(NoContent)
  }
}

