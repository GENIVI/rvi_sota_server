/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.Refined
import org.genivi.sota.resolver.types.Filter
import org.genivi.sota.rest.{ErrorRepresentation, ErrorCodes}
import spray.json.DefaultJsonProtocol._


class FiltersResourceSpec extends ResourceWordSpec {

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

    "not accept duplicate filter names" in {
      addFilter(filterName, filterExpr) ~> route ~> check {
        status shouldBe StatusCodes.Conflict
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.DuplicateEntry
      }
    }

  }
}
