/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.CirceSupport._
import io.circe.generic.auto._
import org.genivi.sota.marshalling.RefinementError

/**
  * When validation, JSON deserialisation fail or a duplicate entry
  * occures in the database, we complete the request by returning the
  * correct status code and JSON error message (see Errors.scala).
  */

object Handlers {

  case class InvalidEntity(msg: String) extends Throwable(msg)

  case object DuplicateEntry extends Throwable("Entry already exists")

  def rejectionHandler : RejectionHandler = RejectionHandler.newBuilder().handle {
    case ValidationRejection(msg, None) =>
      complete( StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, msg) )
  }.handle{
    case MalformedRequestContentRejection(_, Some(RefinementError(_, msg))) =>
      complete(StatusCodes.BadRequest -> ErrorRepresentation(ErrorCodes.InvalidEntity, msg))
  }.result().withFallback(RejectionHandler.default)

  def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case err: java.sql.SQLIntegrityConstraintViolationException if err.getErrorCode == 1062 =>
      complete(StatusCodes.Conflict ->
        ErrorRepresentation(ErrorCodes.DuplicateEntry, "Entry already exists"))
  }
}
