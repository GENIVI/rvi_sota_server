/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import org.scalatest.FlatSpec


class FilterParserSpec extends FlatSpec {

  import org.genivi.sota.resolver.types.FilterParser._
  import org.genivi.sota.resolver.types.FilterAST
  import org.genivi.sota.resolver.types.{And, Or, VinMatches}

  val apaS = s"""vin_matches "apa""""
  val apaF = VinMatches("apa")

  val bepaS = s"""vin_matches "bepa""""
  val bepaF = VinMatches("bepa")

  "The filter parser" should "parse VIN matches" in {
    assert(parseFilter(apaS) == Right(apaF))
  }

  it should "parse conjunctions of filters" in {
    assert(parseFilter(s"$apaS AND $apaS") == Right(And(apaF, apaF)))
  }

  it should "parse disjunctions of filters" in {
    assert(parseFilter(s"$apaS OR $apaS") == Right(Or(apaF, apaF)))
  }

  it should "parse conjunctions with higher precedence than disjunction" in {
    assert(parseFilter(s"$apaS AND $bepaS OR $apaS")
      == Right(Or(And(apaF, bepaF), apaF)))
  }

  it should "allow the precedence to be changed by use of parenthesis" in {
    assert(parseFilter(s"$apaS AND ($bepaS OR $apaS)")
      == Right(And(apaF, Or(bepaF, apaF))))
  }

}
