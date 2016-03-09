package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.unmarshalling._
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.rest.{ErrorRepresentation, ErrorCodes}

/**
 * Spec for Validate REST actions
 */
class ValidateResourceSpec extends ResourceWordSpec {

  "Validate resource" should {

    val filter = Filter(Refined.unsafeApply("myfilter"), Refined.unsafeApply(s"""vin_matches "SAJNX5745SC......""""))

    "accept valid filters" in {
      validateFilter(filter) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "reject filters with empty names" in {
      validateFilter(filter.copy(name = Refined.unsafeApply(""))) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "reject filters with bad filter expressions" in {
      validateFilter(filter.copy(expression = Refined.unsafeApply(filter.expression.get + " AND ?"))) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

  }
}
