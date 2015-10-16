/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.datatype

import cats.{Show, Eq}
import eu.timepit.refined.{Predicate, Refined}
import org.scalacheck.{Arbitrary, Gen}


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

  implicit val PackageIdOrdering: Ordering[Id] = new Ordering[Id] {
    override def compare(id1: Id, id2: Id): Int =
      id1.name.get + id1.version.get compare id2.name.get + id2.version.get
  }

  implicit val showInstance: Show[Id] =
    Show.show(id => s"${id.name.get}-${id.version.get}")

  implicit val eqInstance: Eq[Id] =
    Eq.fromUniversalEquals[Id]

  val genPackageId: Gen[Id] =
    for {
      name    <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaNumChar).map(cs => Refined(cs.mkString))
              : Gen[Name]
      version <- Gen.listOfN(3, Gen.choose(0, 999)).map(_.mkString(".")).map(Refined(_)): Gen[Version]
    } yield Id(name, version)

  implicit lazy val arbPackageId: Arbitrary[Id] =
    Arbitrary(genPackageId)

}
