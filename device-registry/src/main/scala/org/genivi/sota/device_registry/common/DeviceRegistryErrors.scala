/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.common

object DeviceRegistryErrors {
  import akka.http.scaladsl.model.StatusCodes
  import akka.http.scaladsl.server.Directives.complete
  import akka.http.scaladsl.server.ExceptionHandler.PF
  import scala.util.control.NoStackTrace
  import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
  import org.genivi.sota.marshalling.CirceMarshallingSupport._

  object Codes {
    val MissingDevice = ErrorCode("missing_device")
    val ConflictingDevice = ErrorCode("conflicting_device")
  }

  case object MissingDevice extends Throwable with NoStackTrace
  case object ConflictingDevice extends Throwable with NoStackTrace

  val errorHandler: PF = {
    case DeviceRegistryErrors.MissingDevice =>
      complete(StatusCodes.NotFound -> ErrorRepresentation(Codes.MissingDevice, "device doesn't exist"))

    case DeviceRegistryErrors.ConflictingDevice =>
      complete(StatusCodes.Conflict ->
        ErrorRepresentation(Codes.ConflictingDevice, "deviceId or deviceName is already in use"))
  }
}
