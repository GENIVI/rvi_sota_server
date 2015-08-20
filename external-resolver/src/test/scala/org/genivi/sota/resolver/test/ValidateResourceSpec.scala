package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.Refined
import org.genivi.sota.resolver.types.Filter
import org.genivi.sota.rest.{ErrorRepresentation, ErrorCodes}


class ValidateResourceSpec extends ResourceWordSpec {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  "Validate resource" should {

    val filter = Filter(Refined("myfilter"), Refined(s"""vin_matches "SAJNX5745SC??????""""))

    "accept valid filters" in {
      Post(ValidateUri("filter"), filter) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "reject filters with empty names" in {
      Post(ValidateUri("filter"), filter.copy(name = Refined(""))) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "reject filters with bad filter expressions" in {
      Post(ValidateUri("filter"), filter.copy(expression = Refined(filter.expression + " AND ?"))) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

  }
}
