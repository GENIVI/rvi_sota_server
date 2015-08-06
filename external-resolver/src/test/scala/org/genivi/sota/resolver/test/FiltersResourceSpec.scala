package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import org.genivi.sota.resolver.types.Filter
import org.genivi.sota.resolver.rest.{ErrorRepresentation, ErrorCodes}


class FiltersResourceSpec extends ResourceWordSpec {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  "Filters resource" should {

    "create a new resource on POST request" in {
      Post(FiltersUri, Filter(None, "myfilter", s"""vin_matches "SAJNX5745SC??????"""")) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "not accept empty filter names" in {
      Post(FiltersUri, Filter(None, "", s"""vin_matches "SAJNX5745SC??????"""")) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept grammatically wrong expressions" in {
      Post(FiltersUri, Filter(None, "myfilter", s"""vin_matches "SAJNX5745SC??????" AND""")) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

  }
}
