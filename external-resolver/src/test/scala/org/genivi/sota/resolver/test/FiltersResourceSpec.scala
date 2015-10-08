/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.unmarshalling._
import eu.timepit.refined.Refined
import io.circe._
import io.circe.generic.auto._
import org.genivi.sota.rest.ErrorCodes
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.resolver.filters.FilterAST._
import org.genivi.sota.resolver.packages.PackageFilter
import org.genivi.sota.rest.{ErrorRepresentation, ErrorCode}

class FiltersResourceWordSpec extends ResourceWordSpec {

  "Filters resource" should {

    val filterName = "myfilter"
    val filterExpr = s"""vin_matches "SAJNX5745SC......""""
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
    val filterExpr2 = s"""vin_matches "TAJNX5745SC......""""
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

    "fail if the same filter is posted twice" in {
      addFilter(filterName, filterExpr) ~> route ~> check {
        status shouldBe StatusCodes.Conflict
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.DuplicateEntry
      }
    }

    "update the expression of existing filter on PUT request" in {
      updateFilter(filterName, filterExpr) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "fail on trying to update non-existing filters" in {
      updateFilter("nonexistant", filterExpr) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe ErrorCode("filter_not_found")
      }
    }

    "delete filters on DELETE requests" in {
      deleteFilterOK(filterName)

      listFilters ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Filter]] shouldBe List(filter2)
      }
    }

    "fail on trying to delete non-existing filters" in {
      deleteFilter("nonexistant") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe ErrorCode("filter_not_found")
      }
    }

  }
}

object ArbitraryFilter {

  import ArbitraryFilterAST.arbFilterAST
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
    } yield Filter(Refined(name), Refined(ppFilter(expr)))

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
