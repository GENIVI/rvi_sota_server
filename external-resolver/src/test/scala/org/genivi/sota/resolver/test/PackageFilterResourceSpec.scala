/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.Refined
import org.genivi.sota.refined.SprayJsonRefined._
import org.genivi.sota.resolver.types.Package.Metadata
import org.genivi.sota.resolver.types.{Package, Filter, PackageFilter}
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}
import spray.json.DefaultJsonProtocol._


class PackageFilterResourceWordSpec extends ResourceWordSpec {

  "Package filter resource" should {

    val pf = PackageFilter(Refined("package"), Refined("1.0.0"), Refined("filter"))

    "be able to assign exisiting filters to existing packages" in {

      addPackage(pf.packageName.get, pf.packageVersion.get, None, None) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Post(FiltersUri, Filter(pf.filterName, Refined(s"""vin_matches "^X.*""""))) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
      Post(PackageFiltersUri, pf) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[PackageFilter] shouldBe pf
      }
    }

    "not allow assignment of filters to non-existing packages " in {
      Post(PackageFiltersUri, pf.copy(packageName = Refined("nonexistant"))) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe PackageFilter.MissingPackage
      }
    }

    "not allow assignment of non-existing filters to existing packages " in {
      Post(PackageFiltersUri, pf.copy(filterName = Refined("nonexistant"))) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe PackageFilter.MissingFilter
      }
    }

    "list existing package filters on GET requests" in {
      Get(PackageFiltersUri) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[PackageFilter]] shouldBe List(pf)
      }
    }

    "list packages associated to a filter on GET requests to /packagesFor/:filterName" in {
      Get(PackageFiltersListUri("packagesFor", "filter")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Package.Name]] shouldBe List(pf.packageName)
      }
    }

    "list filters associated to a package on GET requests to /filtersFor/:packageName" in {
      Get(PackageFiltersListUri("filtersFor", "package", "1.0.0")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Filter]] shouldBe List(Filter(pf.filterName, Refined(s"""vin_matches "^X.*"""")))
      }
    }

  }
}
