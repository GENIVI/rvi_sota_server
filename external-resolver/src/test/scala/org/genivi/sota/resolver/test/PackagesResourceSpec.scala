/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ValidationRejection
import akka.http.scaladsl.testkit.RouteTestTimeout
import eu.timepit.refined.api.Refined
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.data.DeviceGenerators._
import org.genivi.sota.data.UuidGenerator._
import org.genivi.sota.data.{Device, Namespaces, PackageId, Uuid}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.Errors.Codes
import org.genivi.sota.resolver.db.{Package, PackageResponse}
import org.genivi.sota.resolver.db.Package._
import org.genivi.sota.resolver.test.generators.PackageGenerators
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}
import org.genivi.sota.data.DeviceGenerators._
import scala.concurrent.duration._

/**
 * Spec for Packages REST actions
 */
class PackagesResourcePropSpec extends ResourcePropSpec with PackageGenerators {

  property("create a new resource on PUT request") {
    forAll { (p: Package) =>
      addPackage(p.namespace, p.id.name.get, p.id.version.get, p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[PackageResponse] shouldBe PackageResponse(p.id, p.description, p.vendor)
      }
    }
  }

  property("not accept empty package names") {
    forAll { (p: Package) =>
      addPackage(p.namespace, "", p.id.version.get, p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  property("not accept empty package version") {
    forAll { (p: Package) =>
      addPackage(p.namespace, p.id.name.get, "", p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

  }

  property("not accept bad package versions") {

    forAll { (p: Package, version: String) =>
      addPackage(p.namespace, p.id.name.get, version + ".0", p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity

        // XXX: Fix
        // error.description shouldBe "Predicate failed: Invalid version format."
      }
    }
  }

  property("PUTting the same package twice should update it") {
    forAll { (p: Package) =>
      addPackage(p.namespace, p.id.name.get, p.id.version.get, p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      addPackage(p.namespace, p.id.name.get, p.id.version.get, p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  property("Posting to affected packages returns affected devices") {
    forAll { (device: Uuid, p: Package) =>
      addVehicle(device) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }

      addPackage(p.namespace, p.id.name.get, p.id.version.get, p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }

      installPackage(device, p) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }

      getAffected(defaultNs, Set(p.id)) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Map[Uuid, Seq[PackageId]]] should contain(device -> Seq(p.id))
      }
    }
  }

  property("Posting to affected foreign packages returns affected devices") {
    forAll { (device: Uuid, p: Package) =>
      addVehicle(device) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }

      installFirmware(device, Set(p.id), Set.empty) ~> route ~> check {
        status shouldBe StatusCodes.NoContent
      }

      getAffected(defaultNs, Set(p.id)) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Map[Uuid, Seq[PackageId]]] shouldBe Map(device -> Seq(p.id))
      }
    }
  }
}

/**
 * Spec for Packages REST action word processing
 */
class PackagesResourceWordSpec extends ResourceWordSpec with Namespaces {

  "Packages resource" should {

    "be able to handle unicode descriptions" in {
      addPackage(defaultNs, "name", "1.0.0", Some("嚢"), None) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[PackageResponse] shouldBe
          PackageResponse(PackageId(Refined.unsafeApply("name"), Refined.unsafeApply("1.0.0")), Some("嚢"), None)
      }
    }

    val pkg = PackageResponse(PackageId(Refined.unsafeApply("apa"), Refined.unsafeApply("1.0.0")), None, None)

    "GET /packages/:pkgName/:pkgVersion should return the package or fail" in {
      addPackage(defaultNs, pkg.id.name.get, pkg.id.version.get, None, None) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Get(Resource.uri("packages", pkg.id.name.get, pkg.id.version.get)) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[PackageResponse] shouldBe pkg
      }
      Get(Resource.uri("packages", "bepa", "1.0.0")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.MissingEntity
      }
    }
  }
}
