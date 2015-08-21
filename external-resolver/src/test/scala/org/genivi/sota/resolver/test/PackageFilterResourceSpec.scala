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

    val pkgName    = "package"
    val pkgVersion = "1.0.0"
    val filterName = "filter"
    val filterExpr = s"""vin_matches "^X.*""""
    val pkgFilter  =  PackageFilter(Refined(pkgName), Refined(pkgVersion), Refined(filterName))

    "be able to assign exisiting filters to existing packages" in {
      addPackageOK(pkgName, pkgVersion, None, None)
      addFilterOK(filterName, filterExpr)
      addPackageFilterOK("package", "1.0.0", "filter")
    }

    "not allow assignment of filters to non-existing packages " in {
      addPackageFilter("nonexistant", pkgVersion, filterName) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe PackageFilter.MissingPackage
      }
    }

    "not allow assignment of non-existing filters to existing packages " in {
      addPackageFilter(pkgName, pkgVersion, "nonexistant") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[ErrorRepresentation].code shouldBe PackageFilter.MissingFilter
      }
    }

    "list existing package filters on GET requests" in {
      listPackageFilters ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[PackageFilter]] shouldBe List(pkgFilter)
      }
    }

    "list packages associated to a filter on GET requests to /packagesFor/:filterName" in {
      listPackagesForFilter(filterName) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Package.Name]] shouldBe List(Refined(pkgName))
      }
    }

    "list filters associated to a package on GET requests to /filtersFor/:packageName" in {
      listFiltersForPackage(pkgName, pkgVersion) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Filter]] shouldBe List(Filter(Refined(filterName), Refined(filterExpr)))
      }
    }

  }
}
