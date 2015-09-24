/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import cats.data.Xor
import eu.timepit.refined.Refined
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import CirceMarshallingSupport._
import org.genivi.sota.resolver.Errors.Codes
import org.genivi.sota.resolver.types.{Vehicle, Package, PackageFilter}
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}
import org.scalacheck._


object ArbitraryVehicle {

  val genVehicle: Gen[Vehicle] =
    Gen.listOfN(17, Gen.alphaNumChar).
      map(xs => Vehicle(Refined(xs.mkString)))

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
      map(x => Vehicle(Refined(x)))
}

class VehiclesResourcePropSpec extends ResourcePropSpec {

  import ArbitraryVehicle.{arbVehicle, genInvalidVehicle}

  property("Vehicles resource should create new resource on PUT request") {
    forAll { vehicle: Vehicle =>
      addVehicleOK(vehicle.vin.get)
    }
  }

  property("Invalid vehicles are rejected") {
    forAll(genInvalidVehicle) { vehicle: Vehicle =>
      addVehicle(vehicle.vin.get) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }
  }

  property("PUTting the same vin twice updates it") {
    forAll { vehicle: Vehicle  =>
      addVehicleOK(vehicle.vin.get)
      addVehicleOK(vehicle.vin.get)
    }
  }

}

class VehiclesResourceWordSpec extends ResourceWordSpec {

  "Vin resource" should {

    "create a new resource on PUT request" in {
      addVehicleOK("VINOOLAM0FAU2DEEP")
    }

    "not accept too long Vins" in {
      addVehicle("VINOOLAM0FAU2DEEP1") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept too short Vins" in {
      addVehicle("VINOOLAM0FAU2DEE") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept Vins which aren't alpha num" in {
      addVehicle("VINOOLAM0FAU2DEE!") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "allow duplicate entries" in {
      addVehicleOK("VINOOLAM0FAU2DEEP")
    }

    "list all Vins on a GET request" in {
      listVehicles ~> route ~> check {
        responseAs[Seq[Vehicle]] shouldBe List(Vehicle(Refined("VINOOLAM0FAU2DEEP")))
      }
    }

    /*
     * Tests related to installing packages on VINs.
     */

    "install a package on a VIN on PUT request to /vehicles/:vin/package/:packageName/:packageVersion" in {
      addPackageOK("apa", "1.0.1", None, None)
      Put(Resource.uri("vehicles", "VINOOLAM0FAU2DEEP", "package", "apa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "fail to install a package on a non-existing VIN" in {
      Put(Resource.uri("vehicles", "VINOOLAM0FAU2DEEB", "package", "bepa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Vehicle.MissingVehicle
      }
    }

    "fail to install a non-existing package on a VIN" in {
      Put(Resource.uri("vehicles", "VINOOLAM0FAU2DEEP", "package", "bepa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.PackageNotFound
      }
    }

    "list installed packages on a VIN on GET request to /vehicles/:vin/package" in {
      Get(Resource.uri("vehicles", "VINOOLAM0FAU2DEEP", "package")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Package.Id]] shouldBe List(Package.Id(Refined("apa"), Refined("1.0.1")))
      }
    }

    "fail to list installed packages on VINs that don't exist" in {
      Get(Resource.uri("vehicles", "VINOOLAM0FAU2DEEB", "package")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "uninstall a package on a VIN on DELETE request to /vehicles/:vin/package/:packageName/:packageVersion" in {
      Delete(Resource.uri("vehicles", "VINOOLAM0FAU2DEEP", "package", "apa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Get(Resource.uri("vehicles", "VINOOLAM0FAU2DEEP", "package")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Package.Id]] shouldBe List()
      }
    }

    "fail to uninstall a package from a non-existing VIN" in {
      Delete(Resource.uri("vehicles", "VINOOLAM0FAU2DEEB", "package", "apa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Vehicle.MissingVehicle
      }
    }

    "fail to uninstall a package that isn't installed on a VIN" in {
      Delete(Resource.uri("vehicles", "VINOOLAM0FAU2DEEP", "package", "bepa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.PackageNotFound
      }
    }
  }
}
