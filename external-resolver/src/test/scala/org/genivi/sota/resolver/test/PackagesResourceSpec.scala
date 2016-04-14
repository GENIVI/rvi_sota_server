/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ValidationRejection
import eu.timepit.refined.api.Refined
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.data.{Namespaces, PackageId}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.Errors.Codes
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.packages.Package._
import org.genivi.sota.rest.{ErrorRepresentation, ErrorCodes}


/**
 * Spec for Packages REST actions
 */
class PackagesResourcePropSpec extends ResourcePropSpec with PackageGenerators {

  property("create a new resource on PUT request") {
    forAll { (p : Package) =>
      addPackage(p.id.name.get, p.id.version.get, p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Package] shouldBe p
      }
    }
  }

  property("not accept empty package names") {
    forAll { (p: Package) =>
      addPackage("", p.id.version.get, p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  property("not accept empty package version") {
    forAll { (p: Package) =>
      addPackage(p.id.name.get, "", p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

  }

  property("not accept bad package versions") {

    forAll { (p: Package, version: String) =>
      addPackage(p.id.name.get, version + ".0", p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.InvalidEntity

        // XXX: Fix
        // error.description shouldBe "Predicate failed: Invalid version format."
      }
    }
  }

  property("PUTting the same package twice should update it") {
    forAll { (p: Package) =>
      addPackage(p.id.name.get, p.id.version.get, p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      addPackage(p.id.name.get, p.id.version.get, p.description, p.vendor) ~> route ~> check {
        status shouldBe StatusCodes.OK
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
      addPackage("name", "1.0.0", Some("嚢"), None) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Package] shouldBe
          Package(defaultNs, PackageId(Refined.unsafeApply("name"), Refined.unsafeApply("1.0.0")), Some("嚢"), None)
      }
    }

    val pkg = Package(defaultNs, PackageId(Refined.unsafeApply("apa"), Refined.unsafeApply("1.0.0")), None, None)

    "GET /packages/:pkgName/:pkgVersion should return the package or fail" in {
      addPackage(pkg.id.name.get, pkg.id.version.get, None, None) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Get(Resource.uri("packages", pkg.id.name.get, pkg.id.version.get)) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Package] shouldBe pkg
      }
      Get(Resource.uri("packages", "bepa", "1.0.0")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.PackageNotFound
      }
    }
  }
}
