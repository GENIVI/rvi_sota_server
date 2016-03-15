/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.data

import eu.timepit.refined.api.Refined
import org.scalacheck.{Arbitrary, Gen}
import cats.syntax.show._

/**
  * Created by vladimir on 16/03/16.
  */
trait VinGenerators {
  /**
    * For property based testing purposes, we need to explain how to
    * randomly generate (possibly invalid) VINs.
    *
    * @see [[https://www.scalacheck.org/]]
    */

  val genVin: Gen[Vehicle.Vin] =
    for {
      vin <- SemanticVin.genSemanticVin
    } yield Refined.unsafeApply(vin.show)

  implicit lazy val arbVin: Arbitrary[Vehicle.Vin] =
    Arbitrary(genVin)

  val genVinChar: Gen[Char] =
    Gen.oneOf('A' to 'Z' diff List('I', 'O', 'Q'))

  val genInvalidVin: Gen[Vehicle.Vin] = {

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
       .map(Refined.unsafeApply)
  }

}
