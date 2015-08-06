/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import org.scalacheck._
import org.scalatest.FlatSpec
import org.genivi.sota.resolver.types.FilterParser.parseFilter
import org.genivi.sota.resolver.types.FilterPrinter.ppFilter
import org.genivi.sota.resolver.types.FilterAST
import org.genivi.sota.resolver.types.{And, Or, VinMatches}


class FilterParserSpec extends FlatSpec {

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

  it should "not parse nodes without children" in {
    assert(parseFilter("AND").isLeft
      && parseFilter(s"$apaS OR").isLeft
      && parseFilter(s"$apaS OR AND $apaS").isLeft)
  }

}

object FilterParserPropSpec extends Properties("The filter parser") {

  def genFilterHelper(i: Int): Gen[FilterAST] =
    i match {
      case 0 => for {
        s <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaNumChar)
      } yield VinMatches(s.mkString)
      case n => for {
        l    <- genFilterHelper(i / 2)
        r    <- genFilterHelper(i / 2)
        node <- Gen.oneOf(Or, And)
      } yield node(l, r)
    }

  def genFilter: Gen[FilterAST] = Gen.sized(genFilterHelper)

  property("parses pretty printed filters") =
    Prop.forAll(genFilter) { f: FilterAST =>
      parseFilter(ppFilter(f)) == Right(f)
    }

  property("does not parse junk") =
    Prop.forAll(genFilter) { f: FilterAST =>
      parseFilter(ppFilter(f) + "junk").isLeft
    }
}
