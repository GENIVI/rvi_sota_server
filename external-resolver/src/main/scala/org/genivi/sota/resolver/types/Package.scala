/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.types

case class Package (
  id: Option[Long],
  name: String,
  version: String,
  description: Option[String],
  vendor: Option[String]
)

object Package {
  import eu.timepit.refined._
  import eu.timepit.refined.boolean._
  import eu.timepit.refined.collection._
  import shapeless.tag.@@
  import spray.json.DefaultJsonProtocol._

  implicit val packageFormat = jsonFormat5(Package.apply)

  trait ValidPackageName extends NonEmpty

  trait ValidVersion

  type Valid = ValidPackageName And ValidVersion

  implicit val validPackageName : Predicate[ValidPackageName, Package] = Predicate.instance(p => p.name.nonEmpty, p => "Package name required.")

  implicit val validVersion : Predicate[ValidVersion, Package] = Predicate.instance( _.version.matches( """^\d+\.\d+\.\d+$""" ), _ => "Invalid version format.")

  type ValidPackage = Package @@ Valid

}
