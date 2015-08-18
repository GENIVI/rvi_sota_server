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
import org.genivi.sota.resolver.types.
  {VinMatches, HasPackage, HasComponent, Not, And, Or, True, False}


class FilterParserSpec extends FlatSpec {

  val apaS = s"""vin_matches "apa""""
  val apaF = VinMatches("apa")

  val bepaS = s"""vin_matches "bepa""""
  val bepaF = VinMatches("bepa")

  "The filter parser" should "parse VIN matches" in {
    assert(parseFilter(apaS) == Right(apaF))
  }

  it should "parse has package matches" in {
    assert(parseFilter(s"""has_package "cepa" "1.2.0"""") == Right(HasPackage("cepa", "1.2.0")))
  }

  it should "not parse has package matches without a version" in {
    assert(parseFilter(s"""has_package "cepa" OR $apaS""").isLeft)
  }

  it should "parse conjunctions of filters" in {
    assert(parseFilter(s"$apaS AND $apaS") == Right(And(apaF, apaF)))
  }

  it should "parse disjunctions of filters" in {
    assert(parseFilter(s"$apaS OR $apaS") == Right(Or(apaF, apaF)))
  }

  it should "parse negations of filters" in {
    assert(parseFilter(s"NOT $apaS") == Right(Not(apaF)))
  }

  it should "parse conjunctions with higher precedence than disjunction" in {
    assert(parseFilter(s"$apaS AND $bepaS OR $apaS")
      == Right(Or(And(apaF, bepaF), apaF)))
  }

  it should "parse negation with higher precedence than conjunction" in {
    assert(parseFilter(s"NOT $apaS AND $bepaS")
      == Right(And(Not(apaF), bepaF)))
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

  def genFilterHelper(i: Int): Gen[FilterAST] = {

    def genNullary = Gen.oneOf(True, False)

    def genUnary(n: Int) = for {
        f     <- genFilterHelper(n / 2)
        unary <- Gen.oneOf(Not, Not)
      } yield unary(f)

    def genBinary(n: Int) = for {
        l      <- genFilterHelper(n / 2)
        r      <- genFilterHelper(n / 2)
        binary <- Gen.oneOf(Or, And)
      } yield binary(l, r)

    def genLeaf = Gen.oneOf(
        for {
          s <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaNumChar)
          leaf <- Gen.oneOf(VinMatches, HasComponent)
        } yield leaf(s.mkString),
        for {
          s <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaNumChar)
          t <- Gen.nonEmptyContainerOf[List, Char](Gen.alphaNumChar)
        } yield HasPackage(s.mkString, t.mkString)
    )

    i match {
      case 0 => genLeaf
      case n => Gen.frequency(
        (2, genNullary),
        (8, genUnary(n)),
        (10, genBinary(n))
      )
    }
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
