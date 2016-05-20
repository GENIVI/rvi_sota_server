/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.data

import org.scalacheck.Gen

trait InvalidIdentGenerators {

  val EMPTY_STR = ""

  val genSymbol: Gen[Char] =
    Gen.oneOf('!', '@', '#', '$', '^', '&', '*', '(', ')')

  val genInvalidIdent: Gen[String] =
    for (
      prefix   <- Gen.identifier;
      suffix   <- Gen.identifier
    ) yield prefix + getSymbol + suffix

  def getInvalidIdent: String = genInvalidIdent.sample.getOrElse(getInvalidIdent)

  def getSymbol: Char = genSymbol.sample.getOrElse(getSymbol)

}

object InvalidIdentGenerators extends InvalidIdentGenerators
