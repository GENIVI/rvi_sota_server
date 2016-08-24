/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.http

import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.ExceptionHandler.PF
import akka.http.scaladsl.server.{Directives, ExceptionHandler, _}
import io.circe.Json
import org.genivi.sota.rest.{ErrorCode, ErrorCodes, ErrorRepresentation}
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.marshalling.CirceMarshallingSupport._

import scala.util.control.NoStackTrace

object Errors {
  import Directives._
  import ErrorRepresentation._

  case class MissingEntity(name: Class[_]) extends Throwable with NoStackTrace
  case class EntityAlreadyExists(name: Class[_]) extends Throwable with NoStackTrace

  // TODO: Somehow protect this?
  case class RawError(code: ErrorCode,
                                responseCode: StatusCode,
                                desc: String) extends Exception(desc) with NoStackTrace

  private val onRawError: PF = {
    case RawError(code, statusCode, desc) =>
      complete(statusCode -> ErrorRepresentation(code, desc))
  }

  private val onMissingEntity: PF = {
    case MissingEntity(name) =>
      complete(StatusCodes.NotFound ->
        ErrorRepresentation(ErrorCodes.MissingEntity, s"${name.getSimpleName} not found"))
  }

  private val onConflictingEntity: PF = {
    case EntityAlreadyExists(name) =>
      complete(StatusCodes.Conflict ->
        ErrorRepresentation(ErrorCodes.ConflictingEntity, s"${name.getSimpleName} already exists"))
  }

  private val onIntegrityViolationError: PF = {
    case err: java.sql.SQLIntegrityConstraintViolationException if err.getErrorCode == 1062 =>
      complete(StatusCodes.Conflict ->
        ErrorRepresentation(ErrorCodes.ConflictingEntity, "Entry already exists"))
  }

  // Add more handlers here, or use RawError
  val handleAllErrors =
    Seq(
      onMissingEntity,
      onConflictingEntity,
      onIntegrityViolationError,
      onRawError
    ).foldLeft(PartialFunction.empty[Throwable, Route])(_ orElse _)
}


object ErrorHandler {
  import Directives._
  import Json.obj

  private def defaultHandler(): ExceptionHandler =
    Errors.handleAllErrors orElse ExceptionHandler {
      case e: Throwable =>
        (extractLog & extractUri) { (log, uri) =>
          log.error(e, s"Request to $uri error")
          val entity = obj("error" -> Json.fromString(Option(e.getMessage).getOrElse("")))
          complete(HttpResponse(InternalServerError, entity = entity.toString()))
        }
    }

  val handleErrors: Directive0 = handleExceptions(defaultHandler())
}
