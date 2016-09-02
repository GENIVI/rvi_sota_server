/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.{StatusCodes, Uri}
import eu.timepit.refined.refineV
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.data.{Device, Namespaces, PackageId}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.Errors.Codes
import org.genivi.sota.resolver.packages.{Package, PackageRepository}
import org.genivi.sota.resolver.components.Component
import akka.http.scaladsl.testkit.RouteTestTimeout
import org.genivi.sota.data.Device.DeviceName
import org.genivi.sota.resolver.common.InstalledSoftware
import org.genivi.sota.resolver.test.generators.PackageGenerators
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}
import org.scalacheck._
import Device._
import cats.syntax.show._
import org.scalatest.concurrent.ScalaFutures
import slick.driver.MySQLDriver.api._

import scala.concurrent.duration._


/**
 * Spec for Vehicle REST actions
 */
class VehiclesResourcePropSpec extends ResourcePropSpec
    with PackageGenerators with ScalaFutures {
  import org.genivi.sota.data.DeviceGenerators._

  val devices = "devices"

  import org.scalacheck.Shrink
  implicit val noShrink: Shrink[List[Package]] = Shrink.shrinkAny

  property("updates installed packages even if some of them does not exist") {
    val stateGen : Gen[(Set[Package], Set[Package])] = for {
      beforeUpdate      <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      added             <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      nonExistentAdded  <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      removed           <- Gen.someOf(beforeUpdate)
    } yield (beforeUpdate ++ added, beforeUpdate -- removed ++ added ++ nonExistentAdded)

    forAll(genDevice, stateGen, minSuccessful(3)) { (device, state) =>
      val id = deviceRegistry.createDevice(device.toResponse).futureValue

      val (installedBefore, update) = state
      installedBefore.foreach( p => addPackageOK(p.id.name.get, p.id.version.get, p.description, p.vendor) )
      Put( Resource.uri(devices, id.show, "packages"),
          InstalledSoftware(update.map(_.id), Set())) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }
  }

  property("updates installed packages") {
      val stateGen : Gen[(Set[Package], Set[Package])] = for {
      beforeUpdate <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      added        <- Gen.nonEmptyContainerOf[Set, Package](genPackage)
      removed      <- Gen.someOf(beforeUpdate)
    } yield (beforeUpdate ++ added, beforeUpdate -- removed ++ added)

    forAll(genDevice, stateGen, minSuccessful(3)) { (device, state) =>
      val id = deviceRegistry.createDevice(device.toResponse).futureValue

      val (availablePackages, update) = state
      availablePackages.foreach( p => addPackageOK(p.id.name.get, p.id.version.get, p.description, p.vendor) )
      Put( Resource.uri(devices, id.show, "packages"),
          InstalledSoftware(update.map(_.id), Set())) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }
  }

  property("installs packages as foreign if they are not native") {
    val packageGen = Gen.nonEmptyContainerOf[Set, PackageId](genPackageId)

    forAll(genDevice, packageGen, minSuccessful(3)) { (device, packageIds) =>
      val id = deviceRegistry.createDevice(device.toResponse).futureValue

      Put(Resource.uri(devices, id.show, "packages"),
        InstalledSoftware(packageIds, Set())) ~> route ~> check {
        status shouldBe StatusCodes.NoContent

        Get(Resource.uri(devices, id.show, "package")) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Set[PackageId]] shouldBe packageIds
        }
      }
    }
  }

  property("filters installed packages by regex") {
    val packageGen = Gen.nonEmptyContainerOf[Set, PackageId](genPackageId)

    forAll(genDevice, packageGen, minSuccessful(3)) { (device, packageIds) =>
      val id = deviceRegistry.createDevice(device.toResponse).futureValue

      Put(Resource.uri(devices, id.show, "packages"),
        InstalledSoftware(packageIds, Set())) ~> route ~> check {
        status shouldBe StatusCodes.NoContent

        val query = Uri.Query("regex" -> "doesnotexist")

        Get(Resource.uri(devices, id.show, "package").withQuery(query)) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Set[PackageId]] should be(empty)
        }
      }
    }
  }

  property("filters installed packages by partial regex") {
    forAll(genDevice, genPackageId, minSuccessful(3)) { (device, packageId) =>
      val id = deviceRegistry.createDevice(device.toResponse).futureValue

      Put(Resource.uri(devices, id.show, "packages"),
        InstalledSoftware(Set(packageId), Set())) ~> route ~> check {
        status shouldBe StatusCodes.NoContent

        val partialPackageName = packageId.name.get.headOption.map(_.toString).getOrElse(".*")

        val query = Uri.Query("regex" -> partialPackageName)

        Get(Resource.uri(devices, id.show, "package").withQuery(query)) ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Set[PackageId]] should contain(packageId)
        }
      }
    }
  }

  property("updates foreign packages if already existing") {
    val packageGen = Gen.nonEmptyContainerOf[Set, PackageId](genPackageId)

    forAll(genDevice, packageGen, minSuccessful(3)) { (device, packageIds) =>
      val id = deviceRegistry.createDevice(device.toResponse).futureValue

      val installedSoftware = InstalledSoftware(packageIds, Set())

      Put(Resource.uri(devices, id.show, "packages"), installedSoftware) ~> route ~> check {
        status shouldBe StatusCodes.NoContent

        Put(Resource.uri(devices, id.show, "packages"), installedSoftware) ~> route ~> check {
          status shouldBe StatusCodes.NoContent

          Get(Resource.uri(devices, id.show, "package")) ~> route ~> check {
            status shouldBe StatusCodes.OK
            responseAs[Set[PackageId]] shouldBe packageIds
          }
        }
      }
    }
  }
}

/**
 * Word Spec for Vehicle REST actions
 */
class VehiclesResourceWordSpec extends ResourceWordSpec with Namespaces {

  val devices = "devices"

  val deviceId: Device.Id = refineV[Device.ValidId]("1f22860a-3ea2-491f-9042-37c98c2d51cd").right.map(Device.Id).right.get
  val device: Device     = Device(defaultNs, deviceId, DeviceName("Somename"))

  val deviceId2: Device.Id = refineV[Device.ValidId]("3fd0282d-79f0-455f-9a32-f221790eca3c").right.map(Device.Id).right.get
  val device2: Device     = Device(defaultNs, deviceId, DeviceName("Somename 2"))

  "Vin resource" should {
    "install a package on a VIN on PUT request to /vehicles/:vin/package/:packageName/:packageVersion" in {
      addPackageOK("apa", "1.0.1", None, None)
      installPackageOK(deviceId, "apa", "1.0.1")
    }

    "fail to install a non-existing package on a VIN" in {
      installPackage(deviceId, "bepa", "1.0.1") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.MissingEntity
      }
    }

    "list installed packages on a VIN on GET request to /vehicles/:vin/package" in {
      Get(Resource.uri(devices, deviceId.show, "package")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        val name: PackageId.Name = Refined.unsafeApply("apa")
        val version: PackageId.Version = Refined.unsafeApply("1.0.1")
        responseAs[Seq[PackageId]] shouldBe List(PackageId(name, version))
      }
    }

    "uninstall a package on a VIN on DELETE request to /vehicles/:vin/package/:packageName/:packageVersion" in {
      Delete(Resource.uri(devices, deviceId.show, "package", "apa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Get(Resource.uri(devices, deviceId.show, "package")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[PackageId]] shouldBe List()
      }
    }

    "fail to uninstall a package that isn't installed on a VIN" in {
      Delete(Resource.uri(devices, deviceId.show, "package", "bepa", "1.0.1")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.MissingEntity
      }
    }
  }

  /*
   * Tests related to installing components on VINs.
   */

  "install component on Device Id on PUT /devices/:vin/component/:partNumber" in {
    addComponentOK(Refined.unsafeApply("jobby0"), "nice")
    installComponentOK(deviceId, Refined.unsafeApply("jobby0"))
  }

  "list components on a Device Id on GET /devices/:vin/component" in {
    Get(Resource.uri(devices, deviceId.show, "component")) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[Component.PartNumber]] shouldBe List(Refined.unsafeApply("jobby0"))
    }
  }

  "fail to delete components that are installed on devices" in {
    Delete(Resource.uri("components", "jobby0")) ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
      responseAs[ErrorRepresentation].code shouldBe Codes.ComponentInstalled
    }
  }
}
