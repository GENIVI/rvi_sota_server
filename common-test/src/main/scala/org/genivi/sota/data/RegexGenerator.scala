/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.data

import com.mifmif.common.regex.Generex
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.scalacheck.{Arbitrary, Gen}
import scala.language.implicitConversions


object RegexGenerators {

  // a very simple regex to generate valid regexes
  val regexRegex = (
                     """(                   """ +
                     """  [\|]?             """ + // alternative
                     """  (   [a-zA-Z0-9]+  """ + // simple literals
                     """  | \[[a-ZA-Z0-9]+\]""" + // character classes
                     """  | \([a-ZA-Z0-9]+\)""" + // groups
                     """  ) [\?\+\*]?       """ + // quanttifiers
                     """)*                  """   // repetition
                   )
                   .replace("\n", "")
                   .replace(" ", "")

  val regexGen: Generex = new Generex(regexRegex)

  def genRegex: Gen[String Refined Regex] = Gen.resultOf { _: Unit =>
    Refined.unsafeApply(regexGen.random())
  }

  def genStrFromRegex(regex: String Refined Regex): String =
    new Generex(regex.get).random()

  implicit lazy val arbRegex: Arbitrary[String Refined Regex] = Arbitrary(genRegex)

}
