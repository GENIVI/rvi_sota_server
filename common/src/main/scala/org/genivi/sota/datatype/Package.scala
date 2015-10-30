/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.datatype

import cats.{Show, Eq}
import eu.timepit.refined.{Predicate, Refined}
import org.scalacheck.{Arbitrary, Gen}

/**
  * A (software) package has a notion of id which is shared between the
  * core and the resolver.
  */

trait PackageCommon {

  case class Id(
    name   : Name,
    version: Version
  )

  object Id {
    import io.circe.{Encoder, Decoder}
    import io.circe.generic.semiauto._
    import org.genivi.sota.marshalling.CirceInstances._
    implicit val encoder : Encoder[Id] = deriveFor[Id].encoder
    implicit val decoder : Decoder[Id] = deriveFor[Id].decoder
  }

  /**
    * A valid package id consists of two refined strings, the first
    * being the name of the package and the second being the
    * version. See the predicate below for what constitutes as valid.
    *
    * @see {@link https://github.com/fthomas/refined}
    */

  trait ValidName
  trait ValidVersion
  trait ValidId

  type Name        = Refined[String, ValidName]
  type Version     = Refined[String, ValidVersion]
  type NameVersion = Refined[String, ValidId]

  implicit val validPackageName: Predicate[ValidName, String] =
    Predicate.instance(
      s => s.length > 0 && s.length <= 100
        && s.forall(c => c.isLetter || c.isDigit),
      s => s"$s: isn't a valid package name (between 1 and 100 character long alpha numeric string)"
    )

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

  /**
    * Use the underlaying (string) ordering, show and equality for
    * package ids.
    */

  implicit val PackageIdOrdering: Ordering[Id] = new Ordering[Id] {
    override def compare(id1: Id, id2: Id): Int =
      id1.name.get + id1.version.get compare id2.name.get + id2.version.get
  }

  implicit val showInstance: Show[Id] =
    Show.show(id => s"${id.name.get}-${id.version.get}")

  implicit val eqInstance: Eq[Id] =
    Eq.fromUniversalEquals[Id]

  /**
    * For property based testing purposes, we need to explain how to
    * randomly generate package ids.
    *
    * @see {@link https://www.scalacheck.org/}
    */

  val genPackageId: Gen[Id] =
    for {
      name    <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaNumChar).map(cs => Refined(cs.mkString))
              : Gen[Name]
      version <- Gen.listOfN(3, Gen.choose(0, 999)).map(_.mkString(".")).map(Refined(_)): Gen[Version]
    } yield Id(name, version)

  implicit lazy val arbPackageId: Arbitrary[Id] =
    Arbitrary(genPackageId)

}
