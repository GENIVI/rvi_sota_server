/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

import org.genivi.sota.refined.SprayJsonRefined
import spray.json._

case class PackageId(id: Long) extends AnyVal

case class Package (
  id: Option[PackageId],
  name: Package.PackageName,
  version: Package.Version,
  description: Option[String],
  vendor     : Option[String]
)

object Package {
  import SprayJsonRefined._
  import eu.timepit.refined._
  import spray.json.DefaultJsonProtocol._

  case class Metadata(
    description: Option[String],
    vendor     : Option[String]
  )

  trait Required
  type PackageName = String Refined Required
  implicit val required: Predicate[Required, String]
    = Predicate.instance( _.nonEmpty, _ => "Package name required" )

  trait ValidVersionFormat
  type Version = String Refined ValidVersionFormat
  implicit val validVersion: Predicate[ValidVersionFormat, String] =
    Predicate.instance( _.matches( """^\d+\.\d+\.\d+$""" ), _ => "Invalid version format")


  implicit val packageIdFormat = new JsonFormat[PackageId] {
    override def read(json: JsValue): PackageId = json match {
      case JsNumber(n) => PackageId(n.toLong)
      case x => deserializationError( s"Number expected, '$x' found" )
    }

    override def write(obj: PackageId): JsValue = JsNumber( obj.id )
  }

  implicit val packageMetadataFormat = jsonFormat2(Metadata.apply)

  implicit val packageFormat = new RootJsonFormat[Package] {

    override def write(obj: Package): JsValue = JsObject(
      "name" -> JsString( obj.name.get ),
      "version" -> JsString( obj.version.get ),
      "description" -> implicitly[JsonFormat[Option[String]]].write( obj.description ),
      "vendor" -> implicitly[JsonFormat[Option[String]]].write( obj.vendor )
    )

    override def read(json: JsValue): Package = jsonFormat5(Package.apply).read( json )
  }

}
