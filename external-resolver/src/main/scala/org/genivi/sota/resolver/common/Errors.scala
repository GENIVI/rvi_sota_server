/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.common

object Errors {
  import akka.http.scaladsl.model.StatusCodes
  import akka.http.scaladsl.server.Directives.complete
  import akka.http.scaladsl.server.ExceptionHandler.PF
  import scala.util.control.NoStackTrace
  import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
  import org.genivi.sota.marshalling.CirceMarshallingSupport._

  object Codes {
    val FilterNotFound = ErrorCode("filter_not_found")
    val PackageNotFound = ErrorCode("package_not_found")
  }

  object MissingPackageException extends Throwable with NoStackTrace

  object MissingFilterException extends Throwable with NoStackTrace

  def onMissingFilter : PF = {
    case Errors.MissingFilterException => complete( StatusCodes.NotFound -> ErrorRepresentation( Codes.FilterNotFound, s"Filter not found") )
  }

  def onMissingPackage : PF = {
    case Errors.MissingPackageException => complete( StatusCodes.NotFound -> ErrorRepresentation( Codes.PackageNotFound, "Package not found") )
  }

}
