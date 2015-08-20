/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import spray.json.DefaultJsonProtocol._
import org.genivi.sota.refined.SprayJsonRefined.refinedJsonFormat


case class PackageFilter(
  packageName   : Package.Name,
  packageVersion: Package.Version,
  filterName    : Filter.Name
)

object PackageFilter {

  implicit val packageFilterFormat = jsonFormat3(PackageFilter.apply)

  import org.genivi.sota.rest.ErrorCode

  val MissingPackage = new ErrorCode("missing_package")
  val MissingFilter  = new ErrorCode("missing_filter")
}
