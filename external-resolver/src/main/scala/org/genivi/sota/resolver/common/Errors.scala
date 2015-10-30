/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.common

/**
  * The resolver deals with vehicles, packages, filters and components,
  * sometimes when working with these entities they might not exist, in
  * which case we have to throw an error. This file contains common
  * exceptions and handlers for how to complete requests in which the
  * exceptions are raised.
  */

object Errors {
  import akka.http.scaladsl.model.StatusCodes
  import akka.http.scaladsl.server.Directives.complete
  import akka.http.scaladsl.server.ExceptionHandler.PF
  import scala.util.control.NoStackTrace
  import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
  import org.genivi.sota.marshalling.CirceMarshallingSupport._

  object Codes {
    val FilterNotFound     = ErrorCode("filter_not_found")
    val PackageNotFound    = ErrorCode("package_not_found")
    val MissingVehicle     = ErrorCode("missing_vehicle")
    val MissingComponent   = ErrorCode("missing_component")
    val ComponentInstalled = ErrorCode("component_is_installed")
  }

  case object MissingPackageException       extends Throwable with NoStackTrace

  case object MissingFilterException        extends Throwable with NoStackTrace

  case object MissingVehicle                extends Throwable with NoStackTrace

  case object MissingComponent              extends Throwable with NoStackTrace

  case object ComponentIsInstalledException extends Throwable with NoStackTrace


  val onMissingFilter : PF = {
    case Errors.MissingFilterException =>
      complete( StatusCodes.NotFound -> ErrorRepresentation( Codes.FilterNotFound, s"Filter not found") )
  }

  val onMissingPackage : PF = {
    case Errors.MissingPackageException =>
      complete( StatusCodes.NotFound -> ErrorRepresentation( Codes.PackageNotFound, "Package not found") )
  }

  val onMissingVehicle : PF = {
    case Errors.MissingVehicle =>
      complete(StatusCodes.NotFound -> ErrorRepresentation(Codes.MissingVehicle, "Vehicle doesn't exist"))
  }

  val onMissingComponent : PF = {
    case Errors.MissingComponent =>
      complete(StatusCodes.NotFound -> ErrorRepresentation(Codes.MissingComponent, "Component doesn't exist"))
  }

  val onComponentInstalled: PF = {
    case Errors.ComponentIsInstalledException =>
      complete(StatusCodes.BadRequest ->
        ErrorRepresentation(Codes.ComponentInstalled,
          "Components that are installed on vehicles cannot be removed."))
  }

}
