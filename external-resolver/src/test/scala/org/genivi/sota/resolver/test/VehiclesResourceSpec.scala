/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.{refineMV, refineV}
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.data.{Namespaces, PackageId, Vehicle}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.Errors.Codes
import org.genivi.sota.resolver.packages.{Package, PackageFilter}
import org.genivi.sota.resolver.packages.Package._
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}
import org.scalacheck._


/**
 * Spec for Vehicle REST actions
 */
class VehiclesResourcePropSpec extends ResourcePropSpec
    with PackageGenerators{
  import org.genivi.sota.data.VehicleGenerators._

  val vehicles = "vehicles"

  property("Vehicles resource should create new resource on PUT request") {
    forAll { vehicle: Vehicle =>
      addVehicleOK(vehicle.vin)
    }
  }

  property("Invalid vehicles are rejected") {
    forAll(genInvalidVehicle) { vehicle: Vehicle =>
      addVehicle(vehicle.vin) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }
  }

  property("PUTting the same vin twice updates it") {
    forAll { vehicle: Vehicle  =>
      addVehicleOK(vehicle.vin)
      addVehicleOK(vehicle.vin)
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

  property("updates installed packages even if some of them does not exist") {
    val stateGen : Gen[(Set[Package], Set[Package])] = for {
      beforeUpdate <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      added        <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      nonExistentAdded <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      removed      <- Gen.someOf(beforeUpdate)
    } yield (beforeUpdate ++ added, beforeUpdate -- removed ++ added ++ nonExistentAdded)

    forAll(genVehicle, stateGen) { (vehicle, state) =>
      val (availablePackages, update) = state
      addVehicleOK(vehicle.vin)
      availablePackages.foreach( p => addPackageOK(p.id.name.get, p.id.version.get, p.description, p.vendor) )
      Put( Resource.uri(vehicles, vehicle.vin.get, "packages"),  update.map( _.id )) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }
  }

}

/**
 * Word Spec for Vehicle REST actions
 */
class VehiclesResourceWordSpec extends ResourceWordSpec with Namespaces {

  val vehicles = "vehicles"

  val vin     : Vehicle.Vin = refineV[Vehicle.ValidVin]("V1N00LAM0FAU2DEEP").right.get
  val vehicle : Vehicle     = Vehicle(defaultNs, vin)
  val vin2    : Vehicle.Vin = refineV[Vehicle.ValidVin]("XAPABEPA123456789").right.get
  val vehicle2: Vehicle     = Vehicle(defaultNs, vin2)

  "Vin resource" should {

    "create a new resource on PUT request" in {
      addVehicleOK(vin)
      addVehicleOK(vin2)
    }

    "not accept too long VINs" in {
      addVehicle(Refined.unsafeApply(vin.get + "1")) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept too short VINs" in {
      addVehicle(Refined.unsafeApply(vin.get.drop(1))) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "not accept VINs which aren't alpha num" in {
      addVehicle(Refined.unsafeApply(vin.get.drop(1) + "!")) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity
      }
    }

    "allow duplicate entries" in {
      addVehicleOK(vin)
    }

    "list all VINs on a GET request" in {
      listVehicles ~> route ~> check {
        responseAs[Seq[Vehicle]] shouldBe List(vehicle, vehicle2)
      }
    }

    "list a specific vehicle on GET /vehicles/:vin or fail if it doesn't exist" in {
      Get(Resource.uri("vehicles", vin.get)) ~> route ~> check {
        responseAs[Vehicle] shouldBe vehicle
      }
      Get(Resource.uri("vehicles", vin.get.drop(1) + "1")) ~> route ~> check {
        responseAs[ErrorRepresentation].code shouldBe Codes.MissingVehicle
      }
    }

    "return a 404 when deleting a VIN which doesn't exist" in {
      Delete(Resource.uri(vehicles, "123456789N0TTHERE")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "delete a VIN" in {
      val vin = refineV[Vehicle.ValidVin]("12345678901234V1N").right.get: Vehicle.Vin
      addVehicleOK(vin)
      Delete(Resource.uri(vehicles, vin.get)) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "delete a VIN and its installPackages" in {
      val vin: Vehicle.Vin = refineV[Vehicle.ValidVin]("1234567890THERV1N").right.get
      addVehicleOK(vin)
      addPackageOK("halflife", "3.0.0", None, None)
      installPackageOK(vin, "halflife", "3.0.0")
      addPackageOK("halflife", "4.0.0", None, None)
      installPackageOK(vin, "halflife", "4.0.0")
      Delete(Resource.uri(vehicles, vin.get)) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "delete a VIN and its installComponents" in {
      val vin  = refineV[Vehicle.ValidVin]("1234567890THERV1N").right.get: Vehicle.Vin
      val comp = refineMV("ashtray")          : Component.PartNumber
      addVehicleOK(vin)
      addComponentOK(comp, "good to have")
      installComponentOK(vin, comp)
      Delete(Resource.uri(vehicles, vin.get)) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    /*
     * Tests related to installing packages on VINs.
     */

    "install a package on a VIN on PUT request to /vehicles/:vin/package/:packageName/:packageVersion" in {
      addPackageOK("apa", "1.0.1", None, None)
      installPackageOK(vin, "apa", "1.0.1")
    }

    "fail to install a package on a non-existing VIN" in {
      installPackage(Refined.unsafeApply(vin.get.drop(1) + "B"), "bepa", "1.0.1") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.MissingVehicle
      }
    }

    "fail to install a non-existing package on a VIN" in {
      installPackage(vin, "bepa", "1.0.1") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.PackageNotFound
      }
    }

    "list installed packages on a VIN on GET request to /vehicles/:vin/package" in {
      Get(Resource.uri(vehicles, vin.get, "package")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        val name: PackageId.Name = Refined.unsafeApply("apa")
        val version: PackageId.Version = Refined.unsafeApply("1.0.1")
        responseAs[Seq[PackageId]] shouldBe List(PackageId(name, version))
      }
    }

    "fail to list installed packages on VINs that don't exist" in {
      Get(Resource.uri(vehicles, vin.get.drop(1) + "B", "package")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }

    }

    "uninstall a package on a VIN on DELETE request to /vehicles/:vin/package/:packageName/:packageVersion" in {
      Delete(Resource.uri(vehicles, vin.get, "package", "apa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Get(Resource.uri(vehicles, vin.get, "package")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[PackageId]] shouldBe List()
      }
    }

    "fail to uninstall a package from a non-existing VIN" in {
      Delete(Resource.uri(vehicles, vin.get.drop(1) + "B", "package", "apa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.MissingVehicle
      }
    }

    "fail to uninstall a package that isn't installed on a VIN" in {
      Delete(Resource.uri(vehicles, vin.get, "package", "bepa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.PackageNotFound
      }
    }

    "list all VINs that have a specific package installed on GET request" +
        " to /vehciles?packageName=:packageName&packageVersion=:packageVersion" in {
      installPackageOK(vin, "apa", "1.0.1")
      Get(Resource.uri(vehicles) + "?packageName=apa&packageVersion=1.0.1") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Vehicle]] shouldBe List(vehicle)
      }
    }

    "return the empty list of VINs when the package does not exist" in {
      installPackageOK(vin, "apa", "1.0.1")
      Get(Resource.uri(vehicles) + "?packageName=apa&packageVersion=0.0.0") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Vehicle]] shouldBe List()
      }
    }

    "fail if package parameters ain't provided properly" in {
      installPackageOK(vin, "apa", "1.0.1")
      Get(Resource.uri(vehicles) + "?packageName=&packageVersion=") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

  }

  /*
   * Tests related to installing components on VINs.
   */

  "install component on VIN on PUT /vehicles/:vin/component/:partNumber" in {
    addComponentOK(Refined.unsafeApply("jobby0"), "nice")
    installComponentOK(vin, Refined.unsafeApply("jobby0"))
  }

  "list components on a VIN on GET /vehicles/:vin/component" in {
    Get(Resource.uri(vehicles, vin.get, "component")) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[Component.PartNumber]] shouldBe List(Refined.unsafeApply("jobby0"))
    }
  }

  "list VINs that have a specific component on GET /vehicles?component=:partNumber" in {
    Get(Resource.uri(vehicles) + "?component=jobby0") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[Vehicle]] shouldBe List(vehicle)
    }
    Get(Resource.uri(vehicles) + "?component=jobby1") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[Vehicle]] shouldBe List()
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
