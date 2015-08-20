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

    val filter = Filter(Refined("myfilter"), Refined(s"""vin_matches "SAJNX5745SC??????""""))

    "create a new resource on POST request" in {
      Post(FiltersUri, filter) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "not accept empty filter names" in {
      Post(FiltersUri, Filter(Refined(""), Refined(s"""vin_matches "SAJNX5745SC??????""""))) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept grammatically wrong expressions" in {
      Post(FiltersUri,
        Filter(Refined("myfilter"), Refined(s"""vin_matches "SAJNX5745SC??????" AND"""))) ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
        }
    }

    val filter2 = Filter(Refined("myfilter2"), Refined(s"""vin_matches "TAJNX5745SC??????""""))

    "list available filters on a GET request" in {
      Post(FiltersUri, filter2) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Get(FiltersUri) ~> route ~> check {
        responseAs[Seq[Filter]] shouldBe List(filter, filter2)
      }
    }

    "not accept duplicate filter names" in {
      Post(FiltersUri, filter) ~> route ~> check {
        status shouldBe StatusCodes.Conflict
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.DuplicateEntry
      }
    }

  }
}
