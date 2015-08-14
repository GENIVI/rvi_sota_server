/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import eu.timepit.refined.Refined
import org.genivi.sota.resolver.types.{Filter, PackageFilter}
import org.genivi.sota.resolver.types.Package.Metadata


class PackageFilterResourceWordSpec extends ResourceWordSpec {

  "Package filter resource" should {

    "be able to assign exisiting filters to existing packages" in {
      val pf = PackageFilter(Refined("package"), Refined("1.0.0"), Refined("filter"))

      Put(PackagesUri(pf.packageName.get, pf.packageVersion.get), Metadata(None, None)) ~> route ~> check {
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
  }
}
