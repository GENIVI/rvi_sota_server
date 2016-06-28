/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{Directive0, Directives, ExceptionHandler}
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import io.circe.Json
import org.genivi.sota.rest.ErrorCode

import scala.util.control.NoStackTrace

object ErrorCodes {
  val ExternalResolverError = ErrorCode( "external_resolver_error" )
  val MissingDevice = ErrorCode("missing_device")
}

object Errors {
  case object MissingUpdateSpec extends Throwable with NoStackTrace
}

object ErrorHandler {
  import Directives._
  import Json.{obj, string}

  def defaultHandler(log: LoggingAdapter): ExceptionHandler =
    ExceptionHandler {
      case e: Throwable =>
        extractUri { uri =>
          log.error(s"Request to $uri errored: $e")
          val entity = obj("error" -> Json.fromString(Option(e.getMessage).getOrElse("")))
          complete(HttpResponse(InternalServerError, entity = entity.toString()))
        }
    }

  def handleErrors(implicit log: LoggingAdapter): Directive0 =
    handleExceptions(defaultHandler(log))
}
