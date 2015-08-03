/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.ValidationRejection
import org.genivi.sota.resolver.types.Vin
import org.genivi.sota.resolver.rest.{ErrorRepresentation, ErrorCodes}

class VinResourcePropSpec extends ResourcePropSpec {

  import org.scalacheck._

  val VinGenerator: Gen[Vin] = Gen.listOfN(17, Gen.alphaNumChar).map( xs => Vin(xs.mkString) )

  property("Vin resource add") {
    forAll(VinGenerator) { vin =>
      Post( VinsUri, vin ) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }
}

class VinResourceWordSpec extends ResourceWordSpec {
  import akka.http.scaladsl.server.Directives._

  "Vin resource" should {

    "create a new resource on POST request" in {
      Post( VinsUri, Vin("VINOOLAM0FAU2DEEP") ) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "not accept too long Vins" in {
      Post( VinsUri, Vin("VINOOLAM0FAU2DEEP1") ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity 
      }
    }

    "not accept too short Vins" in {
      Post( VinsUri, Vin("VINOOLAM0FAU2DEE") ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept Vins which aren't alpha num" in {
      Post( VinsUri, Vin("VINOOLAM0FAU2DEE!") ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not allow duplicate entries" in {
      Post( VinsUri, Vin("VINOOLAM0FAU2DEEP") ) ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

  }
}
