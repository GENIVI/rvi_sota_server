/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ValidationRejection
import akka.http.scaladsl.unmarshalling._
import cats.data.Xor
import eu.timepit.refined.Refined
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.types.Package
import org.genivi.sota.resolver.types.Package._
import org.scalacheck._
import org.genivi.sota.rest.{ErrorRepresentation, ErrorCodes}


object ArbitraryPackage {

  val genVersion: Gen[Version] =
    Gen.listOfN(3, Gen.choose(0, 999)).map(_.mkString(".")).map(Refined(_))

  val genPackageName: Gen[Package.Name] =
    Gen.identifier.map(Refined(_))

  val genPackage: Gen[Package] = for {
    name    <- genPackageName
    version <- genVersion
    desc    <- Gen.option(Arbitrary.arbitrary[String])
    vendor  <- Gen.option(Gen.alphaStr)
  } yield Package(Package.Id(name, version), desc, vendor)

  implicit lazy val arbPackage = Arbitrary(genPackage)
}

class PackagesResourcePropSpec extends ResourcePropSpec {

  import ArbitraryPackage.arbPackage

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

class PackagesResourceWordSpec extends ResourceWordSpec {

  "Packages resource" should {

    "be able to handle unicode descriptions" in {
      addPackage("name", "1.0.0", Some("嚢"), None) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Package] shouldBe
          Package(Package.Id(Refined("name"), Refined("1.0.0")), Some("嚢"), None)
      }
    }
  }

}
