package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.Refined
import org.genivi.sota.resolver.types.{Filter, FilterId}
import org.genivi.sota.rest.{ErrorRepresentation, ErrorCodes}


class FiltersResourceSpec extends ResourceWordSpec {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  "Filters resource" should {

    val filter = Filter(None, Refined("myfilter"), Refined(s"""vin_matches "SAJNX5745SC??????""""))

    "create a new resource on POST request" in {
      Post(FiltersUri, filter) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "not accept empty filter names" in {
      Post(FiltersUri, Filter(None, Refined(""), Refined(s"""vin_matches "SAJNX5745SC??????""""))) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept grammatically wrong expressions" in {
      Post(FiltersUri,
        Filter(None, Refined("myfilter"), Refined(s"""vin_matches "SAJNX5745SC??????" AND"""))) ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
        }
    }

    val filter2 = Filter(None, Refined("myfilter2"), Refined(s"""vin_matches "TAJNX5745SC??????""""))

    "list available filters on a GET request" in {
      Post(FiltersUri, filter2) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Get(FiltersUri) ~> route ~> check {
        responseAs[Seq[Filter]] shouldBe List(filter.copy(id = Some(FilterId(1))),
          filter2.copy(id = Some(FilterId(2))))
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
