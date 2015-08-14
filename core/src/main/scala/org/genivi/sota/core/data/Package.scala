/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

case class PackageId( name: Package.Name, version: Package.Version )

object PackageId {
  import spray.json.DefaultJsonProtocol._
  import org.genivi.sota.refined.SprayJsonRefined._

  implicit val protocol = jsonFormat2(PackageId.apply)
}

case class Package(
  id: PackageId,
  description: Option[String],
  vendor: Option[String]
)

object Package {
  import eu.timepit.refined._

  trait ValidName
  trait ValidVersion

  type Name    = String Refined ValidName
  type Version = String Refined ValidVersion

  implicit val validPackageName: Predicate[ValidName, String] =
    Predicate.instance( _.nonEmpty, _ => "Package name required" )

  implicit val validPackageVersion: Predicate[ValidVersion, String] =
    Predicate.instance( _.matches( """^\d+\.\d+\.\d+$""" ), _ => "Invalid version format")


}
