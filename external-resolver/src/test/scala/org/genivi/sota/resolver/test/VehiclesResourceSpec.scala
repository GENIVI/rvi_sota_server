/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.RouteTestTimeout
import eu.timepit.refined.Refined
import eu.timepit.refined.internal.Wrapper
import org.genivi.sota.resolver.types.Vehicle
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}
import org.scalacheck._


object ArbitraryVehicle {

  val genVehicle: Gen[Vehicle] =
    Gen.listOfN(17, Gen.alphaNumChar).
      map(xs => Vehicle(Wrapper.refinedWrapper.wrap(xs.mkString)))

  implicit lazy val arbVehicle: Arbitrary[Vehicle] =
    Arbitrary(genVehicle)

  val genTooLongVin: Gen[String] = for {
    n   <- Gen.choose(18, 100)
    vin <- Gen.listOfN(n, Gen.alphaNumChar)
  } yield vin.mkString

  val genTooShortVin: Gen[String] = for {
    n   <- Gen.choose(1, 16)
    vin <- Gen.listOfN(n, Gen.alphaNumChar)
  } yield vin.mkString

  val genNotAlphaNumVin: Gen[String] =
    Gen.listOfN(17, Arbitrary.arbitrary[Char]).
      suchThat(_.exists(c => !(c.isLetter || c.isDigit))).flatMap(_.mkString)

  val genInvalidVehicle: Gen[Vehicle] =
    Gen.oneOf(genTooLongVin, genTooShortVin, genNotAlphaNumVin).
      map(x => Vehicle(Wrapper.refinedWrapper.wrap(x)))
}

class VehiclesResourcePropSpec extends ResourcePropSpec {

  import ArbitraryVehicle.{arbVehicle, genInvalidVehicle}

  import scala.concurrent.duration._
  implicit val timeout = RouteTestTimeout(5.second)

  property("Vehicles resource should create new resource on PUT request") {
    forAll { vehicle: Vehicle =>
      Put(VehiclesUri(vehicle.vin.get)) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }
  }

  property("Invalid vehicles are rejected") {
    forAll(genInvalidVehicle) { vehicle: Vehicle =>
      Put(VehiclesUri(vehicle.vin.get)) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }
  }

  property("PUTting the same vin twice updates it") {
    forAll { vehicle: Vehicle  =>
      Put(VehiclesUri(vehicle.vin.get)) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
      Put(VehiclesUri(vehicle.vin.get)) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }
  }

}

class VehiclesResourceWordSpec extends ResourceWordSpec {

  "Vin resource" should {

    "create a new resource on PUT request" in {
      Put( VehiclesUri("VINOOLAM0FAU2DEEP") ) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    "not accept too long Vins" in {
      Put( VehiclesUri("VINOOLAM0FAU2DEEP1") ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept too short Vins" in {
      Put( VehiclesUri("VINOOLAM0FAU2DEE") ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept Vins which aren't alpha num" in {
      Put( VehiclesUri("VINOOLAM0FAU2DEE!") ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "allow duplicate entries" in {
      Put( VehiclesUri("VINOOLAM0FAU2DEEP") ) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    "list all Vins on a GET request" in {
      Get(VehiclesUri("")) ~> route ~> check {
        import spray.json.DefaultJsonProtocol._
        responseAs[Seq[Vehicle]] shouldBe List(Vehicle(Refined("VINOOLAM0FAU2DEEP")))
      }
    }

  }
}
