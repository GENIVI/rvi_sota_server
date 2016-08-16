/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.server.{Directive0, Directives, ExceptionHandler, Route}
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.ExceptionHandler.PF
import io.circe.Json
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import org.genivi.sota.marshalling.CirceMarshallingSupport._

import scala.language.existentials
import scala.util.control.NoStackTrace

object ErrorCodes {
  val ExternalResolverError = ErrorCode( "external_resolver_error" )
  val MissingDevice = ErrorCode("missing_device")
  val MissingPackage = ErrorCode("missing_package")
  val BlacklistedPackage = ErrorCode("blacklisted_package")

  val MissingEntity = ErrorCode("missing_entity")
}

object Errors {
  import Directives._

  protected[Errors] case class RawError(code: ErrorCode,
                                        responseCode: StatusCode,
                                        desc: String) extends Exception(desc) with NoStackTrace

  case object MissingUpdateSpec extends Throwable with NoStackTrace
  case class MissingEntity(name: Class[_]) extends Throwable with NoStackTrace

  val BlacklistedPackage = RawError(ErrorCodes.BlacklistedPackage, StatusCodes.BadRequest, "package is blacklisted")
  val MissingPackage = RawError(ErrorCodes.MissingPackage, StatusCodes.NotFound, "package not found")

  private val onMissingEntity: PF = {
    case Errors.MissingEntity(name) =>
      complete(StatusCodes.NotFound ->
        ErrorRepresentation(ErrorCodes.MissingEntity, s"${name.getSimpleName} not found"))
  }

  private val onRawError: PF = {
    case Errors.RawError(code, statusCode, desc) =>
      complete(statusCode -> ErrorRepresentation(code, desc))
  }

  val handleAllErrors =
    Seq(onMissingEntity, onRawError) // Add more handlers here, or use RawError
      .foldLeft(PartialFunction.empty[Throwable, Route])(_ orElse _)
}

object ErrorHandler {
  import Directives._
  import Json.{obj, string}

  def defaultHandler(): ExceptionHandler =
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
