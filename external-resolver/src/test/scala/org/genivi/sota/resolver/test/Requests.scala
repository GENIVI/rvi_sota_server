/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{Uri, HttpRequest, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import CirceMarshallingSupport._
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.resolve.ResolveFunctions
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.resolver.packages.{Package, PackageFilter}
import org.genivi.sota.resolver.vehicles.Vehicle
import org.scalatest.Matchers
import scala.concurrent.duration._

/**
 * Generic test resource object
 * Used in property-based testing
 */
object Resource {
  def uri(pathSuffixes: String*): Uri = {
    val BasePath = Path("/api") / "v1"
    Uri.Empty.withPath(pathSuffixes.foldLeft(BasePath)(_/_))
  }
}

/**
 * Testing Trait for building Vehicle requests
 */
trait VehicleRequests extends Matchers { self: ScalatestRouteTest =>

  def addVehicle(vin: Vehicle.Vin): HttpRequest =
    Put(Resource.uri("vehicles", vin.get))

  def addVehicleOK(vin: Vehicle.Vin)(implicit route: Route): Unit = {

    implicit val routeTimeout: RouteTestTimeout = RouteTestTimeout(5.second)

    addVehicle(vin) ~> route ~> check {
      status shouldBe StatusCodes.NoContent
    }
  }

  def listVehicles: HttpRequest =
    Get(Resource.uri("vehicles"))

  def installPackage(vin: Vehicle.Vin, pname: String, pversion: String): HttpRequest =
    Put(Resource.uri("vehicles", vin.get, "package", pname, pversion))

  def installPackageOK(vin: Vehicle.Vin, pname: String, pversion: String)(implicit route: Route): Unit =
    installPackage(vin, pname, pversion) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

  def installComponent(vin: Vehicle.Vin, part: Component.PartNumber): HttpRequest =
    Put(Resource.uri("vehicles", vin.get, "component", part.get))

  def installComponentOK(vin: Vehicle.Vin, part: Component.PartNumber)
                        (implicit route: Route): Unit =
    installComponent(vin, part) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

}

/**
 * Testing Trait for building Package requests
 */
trait PackageRequests extends Matchers { self: ScalatestRouteTest =>

  def addPackage
    (name: String, version: String, desc: Option[String], vendor: Option[String])
      : HttpRequest
  = Put(Resource.uri("packages", name, version), Package.Metadata(desc, vendor))

  def addPackageOK
    (name: String, version: String, desc: Option[String], vendor: Option[String])
    (implicit route: Route)
      : Unit
  = addPackage(name, version, desc, vendor) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
}

/**
 * Testing Trait for building Component requests
 */
trait ComponentRequests extends Matchers { self: ScalatestRouteTest =>

  def addComponent(part: Component.PartNumber, desc: String): HttpRequest =
    Put(Resource.uri("components", part.get), Component.DescriptionWrapper(desc))

  def addComponentOK(part: Component.PartNumber, desc: String)
                    (implicit route: Route): Unit =
    addComponent(part, desc) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

}

/**
 * Testing Trait for building Filter requests
 */
trait FilterRequests extends Matchers { self: ScalatestRouteTest =>

  def addFilter(name: String, expr: String): HttpRequest =
    Post(Resource.uri("filters"), Filter(Refined.unsafeApply(name), Refined.unsafeApply(expr)))

  def updateFilter(name: String, expr: String): HttpRequest =
    Put(Resource.uri("filters", name), Filter.ExpressionWrapper(Refined.unsafeApply(expr)))

  def addFilterOK(name: String, expr: String)(implicit route: Route): Unit = {

    implicit val routeTimeout: RouteTestTimeout = RouteTestTimeout(5.second)

    addFilter(name, expr) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Filter] shouldBe Filter(Refined.unsafeApply(name), Refined.unsafeApply(expr))
    }
  }

  def updateFilterOK(name: String, expr: String)(implicit route: Route): Unit =
    updateFilter(name, expr) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Filter] shouldBe Filter(Refined.unsafeApply(name), Refined.unsafeApply(expr))
    }

  def deleteFilter(name: String): HttpRequest =
    Delete(Resource.uri("filters", name))

  def deleteFilterOK(name: String)(implicit route: Route): Unit =
    deleteFilter(name) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

  def listFiltersRegex(re: String): HttpRequest =
    Get(Resource.uri("filters") + "?regex=" + re)

  def listFilters: HttpRequest =
    Get(Resource.uri("filters"))

  def validateFilter(filter: Filter): HttpRequest =
    Post(Resource.uri("validate", "filter"), filter)
}

/**
 * Testing Trait for building PackageFilter requests
 */
trait PackageFilterRequests extends Matchers { self: ScalatestRouteTest =>

  def addPackageFilter(pname: String, pversion: String, fname: String): HttpRequest =
    Post(Resource.uri("packageFilters"), PackageFilter(Refined.unsafeApply(pname), Refined.unsafeApply(pversion), Refined.unsafeApply(fname)))

  def addPackageFilterOK(pname: String, pversion: String, fname: String)(implicit route: Route): Unit =
    addPackageFilter(pname, pversion, fname) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[PackageFilter] shouldBe PackageFilter(Refined.unsafeApply(pname), Refined.unsafeApply(pversion), Refined.unsafeApply(fname))
    }

  def listPackageFilters: HttpRequest =
    Get(Resource.uri("packageFilters"))

  def listPackagesForFilter(fname: String): HttpRequest =
    Get(Resource.uri("packageFilters") + s"?filter=$fname")

  def listFiltersForPackage(pname: String, pversion: String): HttpRequest =
    Get(Resource.uri("packageFilters") + s"?packageName=$pname&packageVersion=$pversion")

  def deletePackageFilter(pname: String, pversion: String, fname: String): HttpRequest =
    Delete(Resource.uri("packageFilters", pname, pversion, fname))

  def deletePackageFilterOK(pname: String, pversion: String, fname: String)(implicit route: Route): Unit =
    deletePackageFilter(pname, pversion, fname) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
}

/**
 * Testing Trait for building Resolve requests
 */
trait ResolveRequests extends Matchers { self: ScalatestRouteTest =>

  def resolve(pname: String, pversion: String): HttpRequest =
    Get(Resource.uri("resolve", pname, pversion))

  def resolveOK(pname: String, pversion: String, vins: Seq[Vehicle.Vin])(implicit route: Route): Unit = {


    resolve(pname, pversion) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Map[Vehicle.Vin, List[Package.Id]]] shouldBe
        ResolveFunctions.makeFakeDependencyMap(Package.Id(Refined.unsafeApply(pname), Refined.unsafeApply(pversion)),
          vins.map(Vehicle(_)))
    }
  }
}
