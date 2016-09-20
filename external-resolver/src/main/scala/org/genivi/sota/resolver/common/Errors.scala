/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.common

import org.genivi.sota.http.Errors.{MissingEntity, RawError}
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.data.Firmware
import org.genivi.sota.resolver.db.Package
import org.genivi.sota.resolver.filters.Filter

/**
  * The resolver deals with devices, packages, filters and components,
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
    val ComponentInstalled = ErrorCode("component_is_installed")
    val PackageFilterNotFound = ErrorCode("package_filter_not_found")
  }

  val MissingFilter = MissingEntity(classOf[Filter])
  val MissingPackage = MissingEntity(classOf[Package])
  val MissingFirmware = MissingEntity(classOf[Firmware])
  val MissingComponent = MissingEntity(classOf[Component])

  val ComponentInstalled = RawError(Codes.ComponentInstalled, StatusCodes.BadRequest,
    "Components that are installed on devices cannot be removed.")

  val MissingPackageFilter = RawError(Codes.PackageFilterNotFound, StatusCodes.NotFound,
    "No filter with defined for package")
}
