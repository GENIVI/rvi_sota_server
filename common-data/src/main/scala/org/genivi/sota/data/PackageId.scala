/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.data

import cats.{Eq, Show}
import eu.timepit.refined.api.Validate
import com.typesafe.config.ConfigFactory

case class PackageId(
                 name   : PackageId.Name,
                 version: PackageId.Version
             )

/**
  * A (software) package has a notion of id which is shared between the
  * core and the resolver.
  */

object PackageId {

  import eu.timepit.refined.api.Refined

  import io.circe.generic.semiauto._
  import io.circe.{Decoder, Encoder}
  import org.genivi.sota.marshalling.CirceRefined._
  implicit val encoder : Encoder[PackageId] = deriveFor[PackageId].encoder
  implicit val decoder : Decoder[PackageId] = deriveFor[PackageId].decoder

  /**
    * A valid package id consists of two refined strings, the first
    * being the name of the package and the second being the
    * version. See the predicate below for what constitutes as valid.
    *
    * @see [[https://github.com/fthomas/refined]]
    */

  case class ValidName()
  case class ValidVersion()

  type Name        = Refined[String, ValidName]
  type Version     = Refined[String, ValidVersion]

  implicit val validPackageName: Validate.Plain[String, ValidName] =
    Validate.fromPredicate(
      s => s.length > 0 && s.length <= 100
        && s.forall(c => c.isLetter || c.isDigit || List('-', '+', '.').contains(c)),
      s => s"$s: isn't a valid package name (between 1 and 100 character long alpha numeric string)",
      ValidName()
    )

  implicit val validPackageVersion: Validate.Plain[String, ValidVersion] = {
    val packageFormat = ConfigFactory.load().getString("packages.versionFormat")

    Validate.fromPredicate(
      _.matches(packageFormat),
      _ => "Invalid version format",
      ValidVersion()
    )
  }


  /**
    * Use the underlying (string) ordering, show and equality for
    * package ids.
    */
  implicit val PackageIdOrdering: Ordering[PackageId] = new Ordering[PackageId] {
    override def compare(id1: PackageId, id2: PackageId): Int =
      id1.name.get + id1.version.get compare id2.name.get + id2.version.get
  }

  implicit val showInstance: Show[PackageId] =
    Show.show(id => s"${id.name.get}-${id.version.get}")

  implicit val eqInstance: Eq[PackageId] =
    Eq.fromUniversalEquals[PackageId]

}
