/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import java.util.UUID

import org.genivi.sota.data.{Namespace, PackageId}
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.rest.{GenericResponseEncoder, ResponseEncoder}

/**
 * A case class for packages
 * Packages have an id, a String description and a String vendor
 */
case class Package(
  uuid       : UUID,
  namespace  : Namespace,
  id         : PackageId,
  description: Option[String],
  vendor     : Option[String]
)

case class PackageResponse(id: PackageId,
                           description: Option[String],
                           vendor: Option[String])

object PackageResponse {
  implicit val toResponseEncoder = ResponseEncoder { (pkg: Package) =>
    PackageResponse(pkg.id, pkg.description, pkg.vendor)
  }
}

/**
 * A case class for package filters
 * Filters have a package name, package version and filter name
 */
case class PackageFilter(
  namespace: Namespace,
  packageUuid: UUID,
  filterName: Filter.Name
) {
  override def toString(): String = s"PackageFilter($packageUuid)"
}

case class PackageFilterResponse(
                                  packageName: PackageId.Name,
                                  packageVersion: PackageId.Version,
                                  filterName    : Filter.Name
                                )

object PackageFilterResponse {
  implicit val toResponseConversion = GenericResponseEncoder { (pf: PackageFilter, pkgId: PackageId) =>
    PackageFilterResponse(pkgId.name, pkgId.version, pf.filterName)
  }
}

/**
 * The Package object
 * Represents Packages
 */
object Package {

  case class Metadata(
                       namespace: Namespace,
                       description: Option[String],
                       vendor     : Option[String]
                     )

}


case class PackageStat(packageVersion: PackageId.Version, installedCount: Int)

object PackageStat {
  import io.circe.Encoder
  import io.circe.generic.semiauto._
  import org.genivi.sota.marshalling.CirceMarshallingSupport._
  implicit val encoder: Encoder[PackageStat] = deriveEncoder[PackageStat]
}

