/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.datatype

import eu.timepit.refined.api.{Validate, Refined}
import org.scalacheck.{Arbitrary, Gen}

/*
 * The notion of vehicle has a identification number (VIN), this is
 * shared between the core and resolver.
 */
trait VehicleCommon {

  case class ValidVin()

  /**
    * A valid VIN, see ISO 3779 and ISO 3780, must be 17 letters or
    * digits long and not contain 'I', 'O' or 'Q'. We enforce this at the
    * type level by refining the string type with the following
    * predicate.
    *
    * @see {@link https://github.com/fthomas/refined}
    */
  implicit val validVin : Validate.Plain[String, ValidVin] = Validate.fromPredicate(
    vin => vin.length == 17
        && vin.forall(c => (c.isUpper  || c.isDigit)
                        && (c.isLetter || c.isDigit)
                        && !List('I', 'O', 'Q').contains(c)),
    vin => s"($vin must be 17 letters or digits long and not contain 'I', 'O', or 'Q')",
    ValidVin()
  )

  type Vin = Refined[String, ValidVin]

  implicit val VinOrdering: Ordering[Vin] = new Ordering[Vin] {
    override def compare(v1: Vin, v2: Vin): Int = v1.get compare v2.get
  }

  /**
    * For property based testing purposes, we need to explain how to
    * randomly generate (possibly invalid) VINs.
    *
    * @see {@link https://www.scalacheck.org/}
    */

  val genVinChar: Gen[Char] =
    Gen.oneOf('A' to 'Z' diff List('I', 'O', 'Q'))

  val genVin: Gen[Vin] =
    Gen.listOfN(17, genVinChar).map(cs => Refined.unsafeApply(cs.mkString))

  implicit lazy val arbVin: Arbitrary[Vin] =
    Arbitrary(genVin)

  val genInvalidVin: Gen[Vin] = {

    val genTooLongVin: Gen[String] = for {
      n  <- Gen.choose(18, 100)
      cs <- Gen.listOfN(n, genVinChar)
    } yield cs.mkString

    val genTooShortVin: Gen[String] = for {
      n  <- Gen.choose(1, 16)
      cs <- Gen.listOfN(n, genVinChar)
    } yield cs.mkString

    val genNotAlphaNumVin: Gen[String] =
      Gen.listOfN(17, Arbitrary.arbitrary[Char]).
        suchThat(_.exists(c => !(c.isLetter || c.isDigit))).flatMap(_.mkString)

    Gen.oneOf(genTooLongVin, genTooShortVin, genNotAlphaNumVin)
       .map(Refined.unsafeApply(_))
  }

}
