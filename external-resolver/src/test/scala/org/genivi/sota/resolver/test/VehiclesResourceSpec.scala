/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.Refined
import eu.timepit.refined.internal.Wrapper
import org.genivi.sota.resolver.types.Vehicle
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}

class VinResourcePropSpec extends ResourcePropSpec {

  import org.scalacheck._

  val vehicleGen: Gen[Vehicle] =
    Gen.listOfN(17, Gen.alphaNumChar).map( xs => Vehicle( Wrapper.refinedWrapper.wrap(xs.mkString) ) )

  implicit val arbitraryVehicle: Arbitrary[Vehicle] =
    Arbitrary(vehicleGen)

  property("Vin resource add") {
    forAll { vehicle: Vehicle =>
      Put( VinsUri(vehicle.vin.get) ) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }
  }
}

class VinResourceWordSpec extends ResourceWordSpec {

  "Vin resource" should {

    "create a new resource on PUT request" in {
      Put( VinsUri("VINOOLAM0FAU2DEEP") ) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    "not accept too long Vins" in {
      Put( VinsUri("VINOOLAM0FAU2DEEP1") ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept too short Vins" in {
      Put( VinsUri("VINOOLAM0FAU2DEE") ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept Vins which aren't alpha num" in {
      Put( VinsUri("VINOOLAM0FAU2DEE!") ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "allow duplicate entries" in {
      Put( VinsUri("VINOOLAM0FAU2DEEP") ) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    "list all Vins on a GET request" in {
      Get(VinsUri("")) ~> route ~> check {
        import spray.json.DefaultJsonProtocol._
        responseAs[Seq[Vehicle]] shouldBe List(Vehicle(Refined("VINOOLAM0FAU2DEEP")))
      }
    }

  }
}
