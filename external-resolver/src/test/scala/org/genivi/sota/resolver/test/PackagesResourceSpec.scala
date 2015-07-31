/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ValidationRejection


class PackagesResourceSpec extends ResourceWordSpec {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import org.genivi.sota.resolver.types.Package

  "Package resource" should {

    "create a new resource on POST request" in {
      Post( PackagesUri, Package(None, "name", "version", Some("description"), Some("vendor")) ) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "not accept empty package names" in {
      Post( PackagesUri, Package(None, "", "version", Some("description"), Some("vendor")) ) ~> route ~> check {
        rejection shouldBe a [ValidationRejection]
      }
    }

    "not accept empty package version" in {
      Post( PackagesUri, Package(None, "name", "", Some("description"), Some("vendor")) ) ~> route ~> check {
        rejection shouldBe a [ValidationRejection]
      }
    }

    "accept empty package description" in {
      Post( PackagesUri, Package(None, "name", "version2", None, Some("vendor")) ) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "accept empty package vendor" in {
      Post( PackagesUri, Package(None, "name", "version3", Some("description"), None) ) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "not accept duplicates (name and version have to unique)" in {
      Post( PackagesUri, Package(None, "name", "version", Some("description"), Some("vendor")) ) ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

  }
}
