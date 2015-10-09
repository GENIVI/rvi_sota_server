/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import akka.http.scaladsl.model.Uri
import cats.{Show, Eq}
import cats.data.Xor
import eu.timepit.refined._


case class Package(
  id: Package.Id,
  uri: Uri,
  size: Long,
  checkSum: String,
  description: Option[String],
  vendor: Option[String]
)

object Package {

  case class Id(
    name: Package.Name,
    version: Package.Version
  )

  object Id {
    import io.circe.{Encoder, Decoder}
    import io.circe.generic.semiauto._
    import org.genivi.sota.marshalling.CirceInstances._

    implicit val encoder : Encoder[Id] = deriveFor[Id].encoder
    implicit val decoder : Decoder[Id] = deriveFor[Id].decoder

    implicit val showInstance : Show[Id] = Show.show( x => s"${x.name.get}-${x.version.get}" )

    implicit val eqInstance : Eq[Id] = Eq.fromUniversalEquals[Id]
  }

  trait ValidName
  trait ValidVersion

  type Name    = String Refined ValidName
  type Version = String Refined ValidVersion

  implicit val validPackageName: Predicate[ValidName, String] =
    Predicate.instance( _.nonEmpty, _ => "Package name required" )

  implicit val validPackageVersion: Predicate[ValidVersion, String] =
    Predicate.instance( _.matches( """^\d+\.\d+\.\d+$""" ), x => s"Invalid version format: $x")

}
