/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.Refined
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.Errors.Codes
import org.genivi.sota.resolver.packages.{Package, PackageFilter}
import org.genivi.sota.resolver.packages.Package._
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.vehicles.Vehicle
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

  import ArbitraryVehicle.{genVehicle, arbVehicle, genInvalidVehicle}

  val vehicles = "vehicles"

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

  import org.scalacheck.Shrink
  implicit val noShrink: Shrink[List[Package]] = Shrink.shrinkAny

  property("fail to set installed packages if vin does not exist") {
    forAll(genVehicle, Gen.nonEmptyListOf(genPackage)) { (vehicle, packages) =>
      Put( Resource.uri(vehicles, vehicle.vin.get, "packages"),  packages.map( _.id )) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.MissingVehicle
      }
    }
  }

  property("fail to set installed packages if some of them does not exist") {
    val stateGen : Gen[(Set[Package], Set[Package])] = for {
      beforeUpdate <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      added        <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      removed      <- Gen.someOf(beforeUpdate)
    } yield beforeUpdate -> (beforeUpdate -- removed ++ added)

    forAll(genVehicle, stateGen) { (vehicle, state) =>
      val (installedBefore, update) = state
      addVehicleOK(vehicle.vin.get)
      installedBefore.foreach( p => addPackageOK(p.id.name.get, p.id.version.get, p.description, p.vendor) )
      Put( Resource.uri(vehicles, vehicle.vin.get, "packages"),  update.map( _.id )) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.PackageNotFound
      }
    }
  }

  property("updates installed packages") {
    val stateGen : Gen[(Set[Package], Set[Package])] = for {
      beforeUpdate <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      added        <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      removed      <- Gen.someOf(beforeUpdate)
    } yield (beforeUpdate ++ added, beforeUpdate -- removed ++ added)

    forAll(genVehicle, stateGen) { (vehicle, state) =>
      val (availablePackages, update) = state
      addVehicleOK(vehicle.vin.get)
      availablePackages.foreach( p => addPackageOK(p.id.name.get, p.id.version.get, p.description, p.vendor) )
      Put( Resource.uri(vehicles, vehicle.vin.get, "packages"),  update.map( _.id )) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }
  }

}

class VehiclesResourceWordSpec extends ResourceWordSpec {

  val vehicles = "vehicles"

  "Vin resource" should {

    "create a new resource on PUT request" in {
      addVehicleOK("VINOOLAM0FAU2DEEP")
    }

    "not accept too long VINs" in {
      addVehicle("VINOOLAM0FAU2DEEP1") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept too short VINs" in {
      addVehicle("VINOOLAM0FAU2DEE") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept VINs which aren't alpha num" in {
      addVehicle("VINOOLAM0FAU2DEE!") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "allow duplicate entries" in {
      addVehicleOK("VINOOLAM0FAU2DEEP")
    }

    "list all VINs on a GET request" in {
      listVehicles ~> route ~> check {
        responseAs[Seq[Vehicle]] shouldBe List(Vehicle(Refined("VINOOLAM0FAU2DEEP")))
      }
    }

    "return a 404 when deleteing a VIN which doesn't exist" in {
      Delete(Resource.uri(vehicles, "123456789NOTTHERE")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "delete a VIN" in {
      val vin = "12345678901234VIN"
      addVehicleOK(vin)
      Delete(Resource.uri(vehicles, vin)) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "delete a VIN and its installPackages" in {
      val vin = "123456789OTHERVIN"
      addVehicleOK(vin)
      addPackageOK("halflife", "3.0.0", None, None)
      installPackageOK(vin, "halflife", "3.0.0")
      addPackageOK("halflife", "4.0.0", None, None)
      installPackageOK(vin, "halflife", "4.0.0")
      Delete(Resource.uri(vehicles, vin)) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    /*
     * Tests related to installing packages on VINs.
     */

    "install a package on a VIN on PUT request to /vehicles/:vin/package/:packageName/:packageVersion" in {
      addPackageOK("apa", "1.0.1", None, None)
      installPackageOK("VINOOLAM0FAU2DEEP", "apa", "1.0.1")
    }

    "fail to install a package on a non-existing VIN" in {
      installPackage("VINOOLAM0FAU2DEEB", "bepa", "1.0.1") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.MissingVehicle
      }
    }

    "fail to install a non-existing package on a VIN" in {
      installPackage("VINOOLAM0FAU2DEEP", "bepa", "1.0.1") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.PackageNotFound
      }
    }

    "list installed packages on a VIN on GET request to /vehicles/:vin/package" in {
      Get(Resource.uri(vehicles, "VINOOLAM0FAU2DEEP", "package")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Package.Id]] shouldBe List(Package.Id(Refined("apa"), Refined("1.0.1")))
      }
    }

    "fail to list installed packages on VINs that don't exist" in {
      Get(Resource.uri(vehicles, "VINOOLAM0FAU2DEEB", "package")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }

    }

    "uninstall a package on a VIN on DELETE request to /vehicles/:vin/package/:packageName/:packageVersion" in {
      Delete(Resource.uri(vehicles, "VINOOLAM0FAU2DEEP", "package", "apa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Get(Resource.uri(vehicles, "VINOOLAM0FAU2DEEP", "package")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Package.Id]] shouldBe List()
      }
    }

    "fail to uninstall a package from a non-existing VIN" in {
      Delete(Resource.uri(vehicles, "VINOOLAM0FAU2DEEB", "package", "apa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.MissingVehicle
      }
    }

    "fail to uninstall a package that isn't installed on a VIN" in {
      Delete(Resource.uri(vehicles, "VINOOLAM0FAU2DEEP", "package", "bepa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.PackageNotFound
      }
    }

    "list all VINs that have a specific package installed on GET request to /vehciles?package:packageName-:packageVersion" in {
      installPackageOK("VINOOLAM0FAU2DEEP", "apa", "1.0.1")
      Get(Resource.uri(vehicles) + "?package=apa-1.0.1") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Vehicle.Vin]] shouldBe List(Refined("VINOOLAM0FAU2DEEP"))
      }
    }

    "fail to list VINs that have a specific non-existing package installed" in {
      installPackageOK("VINOOLAM0FAU2DEEP", "apa", "1.0.1")
      Get(Resource.uri(vehicles) + "?package=apa-0.0.0") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.PackageNotFound
      }
    }

    "fail to list VINs that have a specific empty or malformated package installed" in {
      installPackageOK("VINOOLAM0FAU2DEEP", "apa", "1.0.1")
      Get(Resource.uri(vehicles) + "?package=") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

  }

  /*
   * Tests related to installing components on VINs.
   */

  "install component on VIN on PUT /vehicles/:vin/component/:partNumber" in {
    addComponentOK(Refined("jobby0"), "nice")
    installComponentOK(Refined("VINOOLAM0FAU2DEEP"), Refined("jobby0"))
  }

  "list components on a VIN on GET /vehicles/:vin/component" in {
    Get(Resource.uri(vehicles, "VINOOLAM0FAU2DEEP", "component")) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[Component.PartNumber]] shouldBe List(Refined("jobby0"))
    }
  }

  "list VINs that have a specific component on GET /vehicles?component=:partNumber" in {
    Get(Resource.uri(vehicles) + "?component=jobby0") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[Vehicle.Vin]] shouldBe List(Refined("VINOOLAM0FAU2DEEP"))
    }
  }

  "fail to list VINs that have a specific non-existing component installed" in {
    Get(Resource.uri(vehicles) + "?component=jobby1") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
      responseAs[ErrorRepresentation].code shouldBe Codes.MissingComponent
    }
  }

  "fail to list VINs that have a specific empty or malformated component installed" in {
    Get(Resource.uri(vehicles) + "?component=") ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
    }
  }

  "fail to delete components that are installed on vehicles" in {
    Delete(Resource.uri("components", "jobby0")) ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
      responseAs[ErrorRepresentation].code shouldBe Codes.ComponentInstalled
    }
  }

}
