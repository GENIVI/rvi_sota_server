/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import org.genivi.sota.data.PackageId
import org.genivi.sota.resolver.filters.Filter

/**
 * A case class for packages
 * Packages have an id, a String description and a String vendor
 */
case class Package(
  id         : PackageId,
  description: Option[String],
  vendor     : Option[String]
)

/**
 * A case class for package filters
 * Filters have a package name, package version and filter name
 */
case class PackageFilter(
  packageName   : PackageId.Name,
  packageVersion: PackageId.Version,
  filterName    : Filter.Name
)

/**
 * The Package object
 * Represents Packages
 */
object Package {

  case class Metadata(
    description: Option[String],
    vendor     : Option[String]
  )

}
