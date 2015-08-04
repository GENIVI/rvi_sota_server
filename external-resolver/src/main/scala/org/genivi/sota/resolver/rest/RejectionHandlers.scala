/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.rest

object RejectionHandlers {
  import akka.http.scaladsl.model._
  import akka.http.scaladsl.server._
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  implicit def rejectionHandler : RejectionHandler = RejectionHandler.newBuilder().handle {
    case ValidationRejection(msg, None) =>
      complete( StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, msg) )
  }.result().withFallback(RejectionHandler.default)

}
