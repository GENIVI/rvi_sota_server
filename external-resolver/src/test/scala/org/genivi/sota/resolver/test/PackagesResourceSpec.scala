/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ValidationRejection
import eu.timepit.refined.internal.Wrapper
import eu.timepit.refined.internal.Wrapper

class PackageResourceSpec extends ResourcePropSpec {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import org.genivi.sota.resolver.rest.{ErrorRepresentation, ErrorCodes}
  import org.genivi.sota.resolver.types.Package
  import org.genivi.sota.resolver.types.Package._
  import org.scalacheck._

  val VersionGen : Gen[Version] = Gen.listOfN(3, Gen.choose(0, 999)).map( _.mkString(".")).map( Wrapper.refinedWrapper.wrap(_) )

  val PackageNameGen : Gen[PackageName] = Gen.identifier.map( Wrapper.refinedWrapper.wrap(_) )

  val PackageGen : Gen[Package] = for {
    nm <- PackageNameGen
    v  <- VersionGen
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
      Post( PackagesUri, p.copy( name = Wrapper.refinedWrapper.wrap( "" ) )) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        val error = responseAs[ErrorRepresentation]
        error.code shouldBe ErrorCodes.InvalidEntity
        error.description shouldBe "Predicate failed: Package name required."
      }
    }
  }

  property("not accept empty package version") {
    forAll { (p: Package) =>
      Post( PackagesUri, p.copy( version = Wrapper.refinedWrapper.wrap("") )) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        val error = responseAs[ErrorRepresentation]
        error.code shouldBe ErrorCodes.InvalidEntity
        error.description shouldBe "Predicate failed: Invalid version format."
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
