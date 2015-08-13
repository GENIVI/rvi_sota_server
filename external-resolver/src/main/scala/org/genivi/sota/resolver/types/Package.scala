/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import eu.timepit.refined.{Predicate, Refined}
import org.genivi.sota.refined.SprayJsonRefined.refinedJsonFormat
import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.json.JsValue


case class PackageId(id: Long)

case class Package(
  id         : Option[PackageId],
  name       : Package.Name,
  version    : Package.Version,
  description: Option[String],
  vendor     : Option[String]
)

object Package {

  trait ValidName
  trait ValidVersion

  type Name    = String Refined ValidName
  type Version = String Refined ValidVersion

  implicit val validPackageName: Predicate[ValidName, String] =
    Predicate.instance( _.nonEmpty, _ => "Package name required" )

  implicit val validPackageVersion: Predicate[ValidVersion, String] =
    Predicate.instance( _.matches( """^\d+\.\d+\.\d+$""" ), _ => "Invalid version format")


  implicit val packageIdFormat = jsonFormat1(PackageId.apply)
  implicit val packageFormat   = new RootJsonFormat[Package] {

    override def write(obj: Package): JsValue =
      jsonFormat5(Package.apply).write(obj.copy(id = None))

    override def read(json: JsValue): Package =
      jsonFormat5(Package.apply).read(json)
  }

  case class Metadata(
    description: Option[String],
    vendor     : Option[String]
  )

  implicit val packageMetadataFormat = jsonFormat2(Metadata.apply)

}
