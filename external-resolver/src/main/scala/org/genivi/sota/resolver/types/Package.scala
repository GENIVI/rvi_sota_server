/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import eu.timepit.refined.{Predicate, Refined}


case class Package(
  id         : Package.Id,
  description: Option[String],
  vendor     : Option[String]
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

}
