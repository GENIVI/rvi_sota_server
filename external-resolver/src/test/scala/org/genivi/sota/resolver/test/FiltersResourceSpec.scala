/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.Refined
import org.genivi.sota.resolver.types.{Filter, PackageFilter}
import org.genivi.sota.rest.{ErrorRepresentation, ErrorCodes}


class FiltersResourceWordSpec extends ResourceWordSpec {

  import org.genivi.sota.CirceSupport._
  import io.circe.generic.auto._
  import akka.http.scaladsl.unmarshalling._

  "Filters resource" should {

    val filterName = "myfilter"
    val filterExpr = s"""vin_matches "SAJNX5745SC??????""""
    val filter     = Filter(Refined(filterName), Refined(filterExpr))

    "create a new resource on POST request" in {
      addFilterOK(filterName, filterExpr)
    }

    "not accept empty filter names" in {
       addFilter("", filterExpr) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept grammatically wrong expressions" in {
      addFilter(filterName, filterExpr + " AND") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    val filterName2 = "myfilter2"
    val filterExpr2 = s"""vin_matches "TAJNX5745SC??????""""
    val filter2     = Filter(Refined(filterName2), Refined(filterExpr2))

    "list available filters on a GET request" in {
      addFilterOK(filterName2, filterExpr2)
      listFilters ~> route ~> check {
        responseAs[Seq[Filter]] shouldBe List(filter, filter2)
      }
    }

    "allow regex search on listing filters" in {
      listFiltersRegex("^.*2$") ~> route ~> check {
        responseAs[Seq[Filter]] shouldBe List(filter2)
      }
    }

    "Posting the same filter twice should fail" in {
      addFilter(filterName, filterExpr) ~> route ~> check {
        status shouldBe StatusCodes.Conflict
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.DuplicateEntry
      }
    }

    "Putting an existing filter name should update the expression" in {
      updateFilter(filterName, filterExpr) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "Putting an non-existing filter name should fail" in {
      updateFilter("nonexistant", filterExpr) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe PackageFilter.MissingFilter
      }
    }

    "DELETE requests should delete filters" in {
      deleteFilterOK(filterName)

      listFilters ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Filter]] shouldBe List(filter2)
      }
    }

    "Deleting non-existant filters should fail" in {
      deleteFilter("nonexistant") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe PackageFilter.MissingFilter
      }
    }

  }
}

object ArbitraryFilter {

  import ArbitraryFilterAST.arbFilterAST
  import org.genivi.sota.resolver.types.{Filter, FilterPrinter}
  import org.scalacheck._

  val genName: Gen[String] =
    for {
      // We don't want name clashes so keep the names long.
      n  <- Gen.choose(20, 50)
      cs <- Gen.listOfN(n, Gen.alphaNumChar)
    } yield cs.mkString

  val genFilter: Gen[Filter] =
    for {
      name <- genName
      expr <- ArbitraryFilterAST.genFilter
    } yield Filter(Refined(name), Refined(FilterPrinter.ppFilter(expr)))

  implicit lazy val arbFilter = Arbitrary(genFilter)
}

class FiltersResourcePropSpec extends ResourcePropSpec {

  import ArbitraryFilter.arbFilter

  property("Posting random filters should work") {

    forAll { filter: Filter =>
      addFilterOK(filter.name.get, filter.expression.get)
    }
  }

}
