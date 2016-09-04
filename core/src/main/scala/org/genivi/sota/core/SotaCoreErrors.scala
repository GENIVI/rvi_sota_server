/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model.StatusCodes
import org.genivi.sota.http.Errors.RawError
import org.genivi.sota.rest.{ErrorCode, ErrorCodes}

object SotaCoreErrors {
  object SotaCoreErrorCodes {
    val ExternalResolverError = ErrorCode("external_resolver_error")
    val MissingDevice = ErrorCode("missing_device")
    val MissingPackage = ErrorCode("missing_package")
    val BlacklistedPackage = ErrorCode("blacklisted_package")
  }

  val BlacklistedPackage = RawError(SotaCoreErrorCodes.BlacklistedPackage, StatusCodes.BadRequest,
    "package is blacklisted")
  val MissingPackage = RawError(SotaCoreErrorCodes.MissingPackage, StatusCodes.NotFound, "package not found")
  val MissingUpdateSpec = RawError(ErrorCodes.MissingEntity, StatusCodes.NotFound, "update spec not found")
}
