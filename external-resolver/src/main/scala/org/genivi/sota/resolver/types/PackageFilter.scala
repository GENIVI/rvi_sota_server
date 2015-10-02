/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.resolver.packages.Package


case class PackageFilter(
  packageName   : Package.Name,
  packageVersion: Package.Version,
  filterName    : Filter.Name
)

object PackageFilter {
  import io.circe.generic.semiauto._
  import CirceMarshallingSupport._
  implicit val encoderInstance = deriveFor[PackageFilter].encoder
  implicit val decoderInstance = deriveFor[PackageFilter].decoder
}
