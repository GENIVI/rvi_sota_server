/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.common

import org.genivi.sota.http.Errors.RawError

object Errors {
  import akka.http.scaladsl.model.StatusCodes
  import org.genivi.sota.rest.ErrorCode

  object Codes {
    val MissingDevice = ErrorCode("missing_device")
    val ConflictingDeviceId = ErrorCode("conflicting_device_id")
  }

  val MissingDevice = RawError(Codes.MissingDevice, StatusCodes.NotFound, "Device doesn't exist")
  val ConflictingDeviceId = RawError(Codes.ConflictingDeviceId, StatusCodes.Conflict, "deviceId is already in use")

}
