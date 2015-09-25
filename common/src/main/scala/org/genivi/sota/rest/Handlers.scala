/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server._
import io.circe.generic.auto._
import org.genivi.sota.marshalling.{RefinementError, CirceMarshallingSupport}
import CirceMarshallingSupport._
import org.genivi.sota.rest.SotaError._


object Handlers {

  case class InvalidEntity(msg: String) extends SotaError {
    override def getMessage() = msg
  }

  case object DuplicateEntry extends SotaError {
    override def getMessage = "Entry already exists"
  }

  def rejectionHandler : RejectionHandler = RejectionHandler.newBuilder().handle {
    case ValidationRejection(msg, None) =>
      complete(StatusCodes.BadRequest -> InvalidEntity(msg))
  }.handle{
    case MalformedRequestContentRejection(_, Some(RefinementError(_, msg))) =>
      complete(StatusCodes.BadRequest -> InvalidEntity(msg))
  }.result().withFallback(RejectionHandler.default)

  def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case err: java.sql.SQLIntegrityConstraintViolationException if err.getErrorCode == 1062 =>
      complete(StatusCodes.Conflict -> DuplicateEntry)
  }
}