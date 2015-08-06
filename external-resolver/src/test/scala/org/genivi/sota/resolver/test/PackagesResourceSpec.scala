/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ValidationRejection

class PackageResourceSpec extends ResourcePropSpec {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import org.genivi.sota.resolver.rest.{ErrorRepresentation, ErrorCodes}
  import org.genivi.sota.resolver.types.Package
  import org.scalacheck._

  val PackageGen : Gen[Package] = for {
    nm <- Gen.identifier
    v  <- Gen.listOfN(3, Gen.choose(0, 999)).map( _.mkString("."))
    d  <- Gen.option( Arbitrary.arbitrary[String] )
    vnd <- Gen.option( Gen.alphaStr )
  } yield Package(None, nm, v, d, vnd)

  implicit lazy val arbitraryPackage = Arbitrary( PackageGen )

  property("create a new resource on POST request") {
    forAll { (p : Package) =>
      Post( PackagesUri, p ) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  property("not accept empty package names") {
    forAll { (p: Package) =>
      Post( PackagesUri, p.copy( name = "") ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        val error = responseAs[ErrorRepresentation]
        error.code shouldBe ErrorCodes.InvalidEntity
        error.description shouldBe "Package name required."
      }
    }
  }

  property("not accept empty package version") {
    forAll { (p: Package) =>
      Post( PackagesUri, p.copy( version = "") ) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        val error = responseAs[ErrorRepresentation]
        error.code shouldBe ErrorCodes.InvalidEntity
        error.description shouldBe "Invalid version format."
      }
    }
    
  }

  property("not accept duplicates (name and version have to unique)") {
    forAll{ (p: Package) => 
      Post( PackagesUri, p ) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }

      Post( PackagesUri, p ) ~> route ~> check {
        status shouldBe StatusCodes.Conflict
      }
    }
    
  }

}
