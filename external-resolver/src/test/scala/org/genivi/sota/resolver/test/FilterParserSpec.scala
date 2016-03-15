/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import eu.timepit.refined.api.Refined
import org.genivi.sota.data.{PackageId, Vehicle}
import org.scalacheck._
import org.scalatest.FlatSpec
import org.genivi.sota.resolver.filters._
import org.genivi.sota.resolver.filters.FilterAST._

/**
 * Specs for the filter parser
 */
class FilterParserSpec extends FlatSpec {

  val apaS = s"""vin_matches "apa""""
  val apaF = VinMatches(Refined.unsafeApply("apa"))

  val bepaS = s"""vin_matches "bepa""""
  val bepaF = VinMatches(Refined.unsafeApply("bepa"))

  "The filter parser" should "parse VIN matches" in {
    assert(parseFilter(apaS) == Right(apaF))
  }

  it should "parse has package matches" in {
    assert(parseFilter(s"""has_package "cepa" "1.2.0"""") ==
      Right(HasPackage(Refined.unsafeApply("cepa"), Refined.unsafeApply("1.2.0"))))
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

  it should "not parse leaves with valid regexes" in {
    assert(parseFilter(s"""vin_matches "SAJNX5745SC......"""") ===
      Right(VinMatches(Refined.unsafeApply("SAJNX5745SC......"))))
  }

  it should "not parse leaves with invalid regexes" in {
    assert(parseFilter(s"""vin_matches "*" """).isLeft)
  }

}

/**
 * Specs for filter queries
 */
class FilterQuerySpec extends ResourceWordSpec {

  import org.genivi.sota.resolver.packages.Package
  import org.genivi.sota.resolver.components.Component

  val vin1 = Vehicle(Refined.unsafeApply("APABEPA1234567890"))
  val vin2 = Vehicle(Refined.unsafeApply("APACEPA1234567890"))
  val vin3 = Vehicle(Refined.unsafeApply("APADEPA1234567890"))
  val vin4 = Vehicle(Refined.unsafeApply("BEPAEPA1234567890"))
  val vin5 = Vehicle(Refined.unsafeApply("DEPAEPA1234567890"))

  val pkg1 = PackageId(Refined.unsafeApply("pkg1"), Refined.unsafeApply("1.0.0"))
  val pkg2 = PackageId(Refined.unsafeApply("pkg2"), Refined.unsafeApply("1.0.0"))
  val pkg3 = PackageId(Refined.unsafeApply("pkg3"), Refined.unsafeApply("1.0.1"))
  val pkg4 = PackageId(Refined.unsafeApply("pkg4"), Refined.unsafeApply("1.0.1"))

  val part1: Refined[String, Component.ValidPartNumber] = Refined.unsafeApply("part1")
  val part2: Refined[String, Component.ValidPartNumber] = Refined.unsafeApply("part2")
  val part3: Refined[String, Component.ValidPartNumber] = Refined.unsafeApply("part3")
  val part4: Refined[String, Component.ValidPartNumber] = Refined.unsafeApply("part4")

  val vins: Seq[(Vehicle, (Seq[PackageId], Seq[Component.PartNumber]))] =
    List( (vin1, (List(pkg1), List(part1)))
        , (vin2, (List(pkg2), List(part2)))
        , (vin3, (List(pkg3), List(part3)))
        , (vin4, (List(pkg4), List(part4)))
        , (vin5, (List(),     List()))
        )

  def run(f: FilterAST): Seq[Vehicle] =
    vins.filter(query(f)).map(_._1)


  "Filter queries" should {

    "filter by matching VIN" in {
      run(VinMatches(Refined.unsafeApply(".*")))       shouldBe List(vin1, vin2, vin3, vin4, vin5)
      run(VinMatches(Refined.unsafeApply(".*BEPA.*"))) shouldBe List(vin1, vin4)
    }

    "filter by matching package" in {

      // Note that vin5 isn't matched, because it has no components!
      run(HasPackage(Refined.unsafeApply(".*"),       Refined.unsafeApply(".*"))) shouldBe List(vin1, vin2, vin3, vin4)

      run(HasPackage(Refined.unsafeApply(".*"),       Refined.unsafeApply("1.0.0"))) shouldBe List(vin1, vin2)
      run(HasPackage(Refined.unsafeApply("pkg(3|4)"), Refined.unsafeApply(".*")))    shouldBe List(vin3, vin4)

    }

    "filter by matching component" in {
      run(HasComponent(Refined.unsafeApply(".*")))        shouldBe List(vin1, vin2, vin3, vin4)
      run(HasComponent(Refined.unsafeApply("part(1|4)"))) shouldBe List(vin1, vin4)
    }

    "filter by a combination of matching VINs, packages and components" in {
      run(And(VinMatches(Refined.unsafeApply(".*")),
              And(HasPackage(Refined.unsafeApply(".*"), Refined.unsafeApply(".*")),
                  HasComponent(Refined.unsafeApply(".*"))))) shouldBe List(vin1, vin2, vin3, vin4)

      run(And(VinMatches(Refined.unsafeApply(".*BEPA.*")),
              And(HasPackage(Refined.unsafeApply("pkg.*"), Refined.unsafeApply(".*")),
                  HasComponent(Refined.unsafeApply("part4"))))) shouldBe List(vin4)
    }

  }
}

/**
 * Property Spec for the filter parser
 */
object FilterParserPropSpec extends ResourcePropSpec with FilterGenerators {

  property("The filter parser parses pretty printed filters") {
    forAll { f: FilterAST =>
      parseFilter(ppFilter(f)) shouldBe Right(f)
    }
  }

  property("does not parse junk") {
    forAll { f: FilterAST =>
      parseFilter(ppFilter(f) + "junk").isLeft shouldBe true
    }
  }
}
