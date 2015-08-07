/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.rest

import org.genivi.sota.refined.SprayJsonRefined

object RejectionHandlers {
  import akka.http.scaladsl.model._
  import akka.http.scaladsl.server._
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import SprayJsonRefined.RefinmentError

  implicit def rejectionHandler : RejectionHandler = RejectionHandler.newBuilder().handle {
    case ValidationRejection(msg, None) =>
      complete( StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, msg) )
  }.handle{
    case MalformedRequestContentRejection(_, Some(RefinmentError(_, msg))) =>
      complete(StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, msg))
  }.result().withFallback(RejectionHandler.default)

}
