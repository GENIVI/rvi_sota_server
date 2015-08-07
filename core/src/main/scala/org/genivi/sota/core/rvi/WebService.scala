/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core.rvi

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Route

object WebService extends Directives {
  import Protocol._

  def route(deviceCommunication: DeviceCommunication): Route = pathPrefix("rvi") {
    get { complete { NoContent } }
  }
}
