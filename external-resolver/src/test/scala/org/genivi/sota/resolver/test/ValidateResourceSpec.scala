package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.unmarshalling._
import akka.util.ByteString
import cats.data.Xor
import eu.timepit.refined.Refined
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.types.Filter
import org.genivi.sota.rest.{ErrorRepresentation, ErrorCodes}

class ValidateResourceSpec extends ResourceWordSpec {

  "Validate resource" should {

    val filter = Filter(Refined("myfilter"), Refined(s"""vin_matches "SAJNX5745SC......""""))

    "accept valid filters" in {
      validateFilter(filter) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "reject filters with empty names" in {
      validateFilter(filter.copy(name = Refined(""))) ~> route ~> check {
        println( responseAs[ByteString].utf8String )
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "reject filters with bad filter expressions" in {
      validateFilter(filter.copy(expression = Refined(filter.expression.get + " AND ?"))) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

  }
}
