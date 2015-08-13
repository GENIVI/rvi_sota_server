/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import eu.timepit.refined.{Predicate, Refined}
import org.genivi.sota.refined.SprayJsonRefined.refinedJsonFormat
import spray.json.DefaultJsonProtocol._
import spray.json._


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

  type Name    = String Refined ValidName
  type Version = String Refined ValidVersion

  implicit val validPackageName: Predicate[ValidName, String] =
    Predicate.instance( _.nonEmpty, _ => "Package name required" )

  implicit val validPackageVersion: Predicate[ValidVersion, String] =
    Predicate.instance( _.matches( """^\d+\.\d+\.\d+$""" ), _ => "Invalid version format")


  implicit val packageIdFormat       = jsonFormat2(Package.Id.apply)
  implicit val packageMetadataFormat = jsonFormat2(Metadata.apply)
  implicit val packageFormat         = jsonFormat3(Package.apply)

}
