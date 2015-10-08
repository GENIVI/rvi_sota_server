/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import eu.timepit.refined.{Predicate, Refined}
import org.genivi.sota.resolver.filters.Filter


case class Package(
  id         : Package.Id,
  description: Option[String],
  vendor     : Option[String]
)

case class PackageFilter(
  packageName   : Package.Name,
  packageVersion: Package.Version,
  filterName    : Filter.Name
)

object Package {

  case class Id(
    name   : Package.Name,
    version: Package.Version
  )

  case class Metadata(
    description: Option[String],
    vendor     : Option[String]
  )

  trait ValidName
  trait ValidVersion
  trait ValidId

  type Name        = Refined[String, ValidName]
  type Version     = Refined[String, ValidVersion]
  type NameVersion = Refined[String, ValidId]

  implicit val validPackageName: Predicate[ValidName, String] =
    Predicate.instance( _.nonEmpty, _ => "Package name required" )

  implicit val validPackageVersion: Predicate[ValidVersion, String] =
    Predicate.instance( _.matches( """^\d+\.\d+\.\d+$""" ), _ => "Invalid version format")

  implicit val validPackageId: Predicate[ValidId, String] =
    Predicate.instance(s =>
      {
        val nv = s.split("-")
        nv.length == 2 &&
          validPackageName.isValid(nv.head) &&
          validPackageVersion.isValid(nv.tail.head)
      }
      , s => s"Invalid package id (should be package name dash package version): $s")

  implicit val PackageIdOrdering: Ordering[Package.Id] = new Ordering[Package.Id] {
    override def compare(id1: Package.Id, id2: Package.Id): Int =
      id1.name.get + id1.version.get compare id2.name.get + id2.version.get
  }
}
