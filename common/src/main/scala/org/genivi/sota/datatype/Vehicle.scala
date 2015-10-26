/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.datatype

import eu.timepit.refined.{Predicate, Refined}
import org.scalacheck.{Arbitrary, Gen}


trait VehicleCommon {

  trait ValidVin

  implicit val validVin : Predicate[ValidVin, String] = Predicate.instance(
    vin => vin.length == 17 && vin.forall(c => c.isLetter || c.isDigit),
    vin => s"($vin isn't 17 letters or digits long)"
  )

  type Vin = Refined[String, ValidVin]

  implicit val VinOrdering: Ordering[Vin] = new Ordering[Vin] {
    override def compare(v1: Vin, v2: Vin): Int = v1.get compare v2.get
  }

  val genVin: Gen[Vin] =
    Gen.listOfN(17, Gen.alphaNumChar)
       .map(xs => Refined(xs.mkString))

  implicit lazy val arbVin: Arbitrary[Vin] =
    Arbitrary(genVin)

  val genInvalidVin: Gen[Vin] = {

    val genTooLongVin: Gen[String] = for {
      n  <- Gen.choose(18, 100)
      xs <- Gen.listOfN(n, Gen.alphaNumChar)
    } yield xs.mkString

    val genTooShortVin: Gen[String] = for {
      n  <- Gen.choose(1, 16)
      xs <- Gen.listOfN(n, Gen.alphaNumChar)
    } yield xs.mkString

    val genNotAlphaNumVin: Gen[String] =
      Gen.listOfN(17, Arbitrary.arbitrary[Char]).
        suchThat(_.exists(c => !(c.isLetter || c.isDigit))).flatMap(_.mkString)

    Gen.oneOf(genTooLongVin, genTooShortVin, genNotAlphaNumVin)
       .map(Refined(_))
  }

}
