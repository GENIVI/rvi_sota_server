/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ValidationRejection
import eu.timepit.refined.internal.Wrapper
import org.genivi.sota.resolver.types.Package
import org.genivi.sota.resolver.types.Package._
import org.genivi.sota.rest.{ErrorRepresentation, ErrorCodes}
import org.scalacheck._


object ArbitraryPackage {

  val genVersion: Gen[Version] =
    Gen.listOfN(3, Gen.choose(0, 999)).map(_.mkString(".")).map(Wrapper.refinedWrapper.wrap(_))

  val genPackageName: Gen[PackageName] =
    Gen.identifier.map(Wrapper.refinedWrapper.wrap(_))

  val genPackage: Gen[Package] = for {
    name    <- genPackageName
    version <- genVersion
    desc    <- Gen.option(Arbitrary.arbitrary[String])
    vendor  <- Gen.option(Gen.alphaStr)
  } yield Package(None, name, version, desc, vendor)

  implicit lazy val arbPackage = Arbitrary(genPackage)
}

class PackageResourceSpec extends ResourcePropSpec {

  import ArbitraryPackage.arbPackage

  property("create a new resource on PUT request") {
    forAll { (p : Package) =>
      Put( PackagesUri(p.name.get, p.version.get), Metadata(p.description, p.vendor) ) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  property("not accept empty package names") {
    forAll { (p: Package) =>
      Put( PackagesUri("", p.version.get), Metadata(p.description, p.vendor) ) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  property("not accept empty package version") {
    forAll { (p: Package) =>
      Put( PackagesUri(p.name.get, ""), Metadata(p.description, p.vendor) ) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

  }

  property("not accept bad package versions") {
    forAll { (p: Package, version: String) =>
      Put( PackagesUri(p.name.get, version + ".0"), Metadata(p.description, p.vendor) ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        val error = responseAs[ErrorRepresentation]
        error.code shouldBe ErrorCodes.InvalidEntity
        error.description shouldBe "Predicate failed: Invalid version format."
      }
    }
  }

  property("PUTting the same package twice should update it") {
    forAll{ (p: Package) =>
      Put( PackagesUri(p.name.get, p.version.get), Metadata(p.description, p.vendor) ) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Put( PackagesUri(p.name.get, p.version.get), Metadata(p.description, p.vendor) ) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

}
