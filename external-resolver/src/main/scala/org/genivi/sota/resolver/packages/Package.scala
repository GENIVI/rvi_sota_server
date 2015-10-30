/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import org.genivi.sota.datatype.PackageCommon
import org.genivi.sota.resolver.filters.Filter
import org.scalacheck.{Arbitrary, Gen}

/**
 * A case class for packages
 * Packages have an id, a String description and a String vendor
 */
case class Package(
  id         : Package.Id,
  description: Option[String],
  vendor     : Option[String]
)

/**
 * A case class for package filters
 * Filters have a package name, package version and filter name
 */
case class PackageFilter(
  packageName   : Package.Name,
  packageVersion: Package.Version,
  filterName    : Filter.Name
)

/**
 * The Package object
 * Represents Packages
 */
object Package extends PackageCommon {

  case class Metadata(
    description: Option[String],
    vendor     : Option[String]
  )

  val genPackage: Gen[Package] = for {
    id      <- genPackageId
    desc    <- Gen.option(Arbitrary.arbitrary[String])
    vendor  <- Gen.option(Gen.alphaStr)
  } yield Package(id, desc, vendor)

  implicit val arbPackage: Arbitrary[Package] =
    Arbitrary(genPackage)

}
