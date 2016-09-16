/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.data.{Namespaces, PackageId}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.Errors.Codes
import org.genivi.sota.resolver.db.{Package, PackageFilter}
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.resolver.db.PackageFilter
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}

/**
 * Spec for Package Filter REST actions
 */
class PackageFilterResourceWordSpec extends ResourceWordSpec with Namespaces {

  "Package filter resource" should {

    val pkgName    = "package1"
    val pkgVersion = "1.0.0"
    val filterName = "filter"
    val filterExpr = s"""vin_matches "^X.*""""
    val pkgFilter  =
      PackageFilter(
        defaultNs,
        Refined.unsafeApply(pkgName),
        Refined.unsafeApply(pkgVersion),
        Refined.unsafeApply(filterName))

    "be able to assign exisiting filters to existing packages" in {
      addPackageOK(pkgName, pkgVersion, None, None)
      addFilterOK(filterName, filterExpr)
      addPackageFilterOK(pkgName, pkgVersion, filterName)
    }

    "not allow assignment of filters to non-existing package names" in {
      addPackageFilter("nonexistant", pkgVersion, filterName) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.MissingEntity
      }
    }

    "not allow assignment of filters to non-existing package versions" in {
      addPackageFilter(pkgName, "0.0.9", filterName) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.MissingEntity
      }
    }

    "not allow assignment of non-existing filters to existing packages " in {
      addPackageFilter(pkgName, pkgVersion, "nonexistant") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe ErrorCodes.MissingEntity
      }
    }

    "list existing package filters on GET requests" in {
      listPackageFilters ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[PackageFilter]] shouldBe List(pkgFilter)
      }
    }

    "list packages associated to a filter on GET requests to /filters/:filterName/package" in {
      listPackagesForFilter(filterName) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[List[Package]] shouldBe List(
          Package(defaultNs, PackageId(Refined.unsafeApply(pkgName), Refined.unsafeApply(pkgVersion)), None, None))
      }
    }

    "fail to list packages associated to empty filter names" in {
      listPackagesForFilter("") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "fail to list packages associated to non-existant filters" in {
      listPackagesForFilter("nonexistant") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "list filters associated to a package on GET requests to /packages/:pkgName/:pkgVersion/filter" in {
      listFiltersForPackage(pkgName, pkgVersion) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[Filter]] shouldBe List(
          Filter(defaultNs, Refined.unsafeApply(filterName), Refined.unsafeApply(filterExpr)))
      }
    }

    "fail to list filters associated to a package if no package name is given" in {
      listFiltersForPackage("", pkgVersion) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "fail to list filters associated to a package if a non-existant package name is given" in {
      listFiltersForPackage("nonexistant", pkgVersion) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "fail to list filters associated to a package if no package version is given" in {
      listFiltersForPackage(pkgName, "") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "fail to list filters associated to a package if a non-existant package version is given" in {
      listFiltersForPackage(pkgName, "6.6.6") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "delete package filters on DELETE requests" in {
      deletePackageFilter(pkgName, pkgVersion, filterName) ~> route ~> check {
        status shouldBe StatusCodes.OK
        listPackageFilters ~> route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Seq[PackageFilter]] shouldBe List()
        }
      }
    }

    "fail if package filter does not exist" in {
      deletePackageFilter("nonexistant", pkgVersion, filterName) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[ErrorRepresentation].code shouldBe Codes.PackageFilterNotFound
      }
    }

    "delete all package filters when a filter is deleted" in {
      addPackageFilterOK(pkgName, pkgVersion, filterName)
      deleteFilterOK(filterName)
      listPackageFilters ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[PackageFilter]] shouldBe List()
      }
    }


  }
}
