/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types


case class PackageFilter(
  packageName   : Package.Name,
  packageVersion: Package.Version,
  filterName    : Filter.Name
)

object PackageFilter {

  import org.genivi.sota.rest.ErrorCode

  val MissingPackage       = new ErrorCode("missing_package")
  val MissingFilter        = new ErrorCode("missing_filter")
  val MissingPackageFilter = new ErrorCode("missing_package_filter")
}
