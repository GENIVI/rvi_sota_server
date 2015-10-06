/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{Uri, HttpRequest, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import eu.timepit.refined.Refined
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import CirceMarshallingSupport._
import org.genivi.sota.resolver.resolve._
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.resolver.packages.{Package, PackageFilter}
import org.genivi.sota.resolver.vehicles.Vehicle
import org.scalatest.Matchers
import scala.concurrent.duration._


object Resource {
  def uri(pathSuffixes: String*): Uri = {
    val BasePath = Path("/api") / "v1"
    Uri.Empty.withPath(pathSuffixes.foldLeft(BasePath)(_/_))
  }
}

trait VehicleRequests extends Matchers { self: ScalatestRouteTest =>

  def addVehicle(vin: String): HttpRequest =
    Put(Resource.uri("vehicles", vin))

  def addVehicleOK(vin: String)(implicit route: Route): Unit = {

    implicit val routeTimeout: RouteTestTimeout = RouteTestTimeout(5.second)

    addVehicle(vin) ~> route ~> check {
      status shouldBe StatusCodes.NoContent
    }
  }

  def listVehicles: HttpRequest =
    Get(Resource.uri("vehicles"))

  def installPackage(vin: String, pname: String, pversion: String): HttpRequest =
    Put(Resource.uri("vehicles", vin, "package", pname, pversion))

  def installPackageOK(vin: String, pname: String, pversion: String)(implicit route: Route): Unit =
    installPackage(vin, pname, pversion) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

}

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

trait FilterRequests extends Matchers { self: ScalatestRouteTest =>

  def addFilter(name: String, expr: String): HttpRequest =
    Post(Resource.uri("filters"), Filter(Refined(name), Refined(expr)))

  def updateFilter(name: String, expr: String): HttpRequest =
    Put(Resource.uri("filters", name), Filter.ExpressionWrapper(Refined(expr)))

  def addFilterOK(name: String, expr: String)(implicit route: Route): Unit = {

    implicit val routeTimeout: RouteTestTimeout = RouteTestTimeout(5.second)

    addFilter(name, expr) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Filter] shouldBe Filter(Refined(name), Refined(expr))
    }
  }

  def updateFilterOK(name: String, expr: String)(implicit route: Route): Unit =
    updateFilter(name, expr) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Filter] shouldBe Filter(Refined(name), Refined(expr))
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

trait PackageFilterRequests extends Matchers { self: ScalatestRouteTest =>

  def addPackageFilter(pname: String, pversion: String, fname: String): HttpRequest =
    Post(Resource.uri("packageFilters"), PackageFilter(Refined(pname), Refined(pversion), Refined(fname)))

  def addPackageFilterOK(pname: String, pversion: String, fname: String)(implicit route: Route): Unit =
    addPackageFilter(pname, pversion, fname) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[PackageFilter] shouldBe PackageFilter(Refined(pname), Refined(pversion), Refined(fname))
    }

  def listPackageFilters: HttpRequest =
    Get(Resource.uri("packageFilters"))

  def listPackagesForFilter(fname: String): HttpRequest =
    Get(Resource.uri("packageFilters") + s"?filter=$fname")

  def listFiltersForPackage(pname: String, pversion: String): HttpRequest =
    Get(Resource.uri("packageFilters") + s"?package=$pname-$pversion")

  def deletePackageFilter(pname: String, pversion: String, fname: String): HttpRequest =
    Delete(Resource.uri("packageFilters", pname, pversion, fname))

  def deletePackageFilterOK(pname: String, pversion: String, fname: String)(implicit route: Route): Unit =
    deletePackageFilter(pname, pversion, fname) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
}

trait ResolveRequests extends Matchers { self: ScalatestRouteTest =>

  def resolve(pname: String, pversion: String): HttpRequest =
    Get(Resource.uri("resolve", pname, pversion))

  def resolveOK(pname: String, pversion: String, vins: Seq[String])(implicit route: Route): Unit = {


    resolve(pname, pversion) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Map[Vehicle.Vin, List[Package.Id]]] shouldBe
        ResolveFunctions.makeFakeDependencyMap(Package.Id(Refined(pname), Refined(pversion)),
          vins.map(s => Vehicle(Refined(s))))
    }
  }
}
