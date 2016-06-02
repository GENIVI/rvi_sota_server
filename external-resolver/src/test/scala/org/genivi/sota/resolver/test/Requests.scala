/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.client.RequestBuilding.{Delete, Get, Post, Put}
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.data.Namespace.Namespace
import org.genivi.sota.data.{Namespaces, PackageId, Vehicle}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.InstalledSoftware
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.data.Firmware
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.resolver.packages.{Package, PackageFilter}
import org.genivi.sota.resolver.resolve.ResolveFunctions
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext
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
trait VehicleRequestsHttp {

  def addVehicle(vin: Vehicle.Vin): HttpRequest =
    Put(Resource.uri("vehicles", vin.get))

  def installPackage(veh: Vehicle, pkg: Package): HttpRequest =
    installPackage(veh.vin, pkg.id.name.get, pkg.id.version.get)

  def installPackage(vin: Vehicle.Vin, pname: String, pversion: String): HttpRequest =
    Put(Resource.uri("vehicles", vin.get, "package", pname, pversion))

  def uninstallPackage(veh: Vehicle, pkg: Package): HttpRequest =
    uninstallPackage(veh.vin, pkg.id.name.get, pkg.id.version.get)

  def uninstallPackage(vin: Vehicle.Vin, pname: String, pversion: String): HttpRequest =
    Delete(Resource.uri("vehicles", vin.get, "package", pname, pversion))

  def listVehicles: HttpRequest =
    Get(Resource.uri("vehicles"))

  def listVehiclesHaving(cmp: Component): HttpRequest =
    listVehiclesHaving(cmp.partNumber.get)

  def listVehiclesHaving(partNumber: String): HttpRequest =
    Get(Resource.uri("vehicles").withQuery(Query("component" -> partNumber)))

  def listPackagesOnVehicle(veh: Vehicle): HttpRequest =
    Get(Resource.uri("vehicles", veh.vin.get, "package"))

  def listComponentsOnVehicle(veh: Vehicle): HttpRequest =
    listComponentsOnVehicle(veh.vin.get)

  def listComponentsOnVehicle(vin: String): HttpRequest =
    Get(Resource.uri("vehicles", vin, "component"))

  private def path(vin: Vehicle.Vin, part: Component.PartNumber): Uri =
    Resource.uri("vehicles", vin.get, "component", part.get)

  def installComponent(veh: Vehicle, cmpn: Component): HttpRequest =
    installComponent(veh.vin, cmpn.partNumber)

  def installComponent(vin: Vehicle.Vin, part: Component.PartNumber): HttpRequest =
    Put(path(vin, part))

  def uninstallComponent(veh: Vehicle, cmpn: Component): HttpRequest =
    uninstallComponent(veh.vin, cmpn.partNumber)

  def uninstallComponent(vin: Vehicle.Vin, part: Component.PartNumber): HttpRequest =
    Delete(path(vin, part))

}

trait VehicleRequests extends
    VehicleRequestsHttp with
    PackageRequestsHttp with
    Matchers { self: ScalatestRouteTest =>

  def addVehicleOK(vin: Vehicle.Vin)(implicit route: Route): Unit = {

    implicit val routeTimeout: RouteTestTimeout = RouteTestTimeout(5.second)

    addVehicle(vin) ~> route ~> check {
      status shouldBe StatusCodes.NoContent
    }
  }

  def installPackageOK(vin: Vehicle.Vin, pname: String, pversion: String)(implicit route: Route): Unit =
    installPackage(vin, pname, pversion) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

  def installComponentOK(vin: Vehicle.Vin, part: Component.PartNumber)
                        (implicit route: Route): Unit =
    installComponent(vin, part) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

  def uninstallComponentOK(vin: Vehicle.Vin, part: Component.PartNumber)
                          (implicit route: Route): Unit =
    uninstallComponent(vin, part) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

}

/**
  * Testing Trait for building Package requests
  */
trait PackageRequestsHttp {

  def addPackage(pkg: Package)
                (implicit ec: ExecutionContext): HttpRequest =
    addPackage(pkg.namespace, pkg.id.name.get, pkg.id.version.get, pkg.description, pkg.vendor)

  def addPackage(namespace: Namespace, name: String, version: String, desc: Option[String], vendor: Option[String])
                (implicit ec: ExecutionContext): HttpRequest =
    Put(Resource.uri("packages", name, version), Package.Metadata(namespace, desc, vendor))
}

trait PackageRequests extends
  PackageRequestsHttp with Namespaces with Matchers { self: ScalatestRouteTest =>

    def addPackageOK(name: String, version: String, desc: Option[String], vendor: Option[String])
                  (implicit route: Route): Unit =
    addPackage(defaultNs, name, version, desc, vendor) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
}

/**
 * Testing Trait for building Firmware requests
 */
trait FirmwareRequests extends Matchers { self: ScalatestRouteTest =>

  def installFirmware
    (vin: Vehicle.Vin, packages: Set[PackageId], firmware: Set[Firmware])
      : HttpRequest
  = Put(Resource.uri("vehicles", vin.get, "packages"), InstalledSoftware(packages, firmware))

  def installFirmwareOK
    (vin: Vehicle.Vin, packages: Set[PackageId], firmware: Set[Firmware])
    (implicit route: Route)
      : Unit
  = installFirmware(vin, packages, firmware) ~> route ~> check {
      status shouldBe StatusCodes.NoContent
    }
}

/**
 * Testing Trait for building Component requests
 */
trait ComponentRequestsHttp {

  def listComponents: HttpRequest =
    Get(Resource.uri("components"))

  def addComponent(cmpn: Component)
                  (implicit ec: ExecutionContext): HttpRequest =
    addComponent(cmpn.partNumber, cmpn.description)

  def addComponent(part: Component.PartNumber, desc: String)
                  (implicit ec: ExecutionContext): HttpRequest =
    Put(Resource.uri("components", part.get), Component.DescriptionWrapper(desc))

  def deleteComponent(cmpn: Component): HttpRequest =
    deleteComponent(cmpn.partNumber)

  def deleteComponent(part: Component.PartNumber): HttpRequest =
    Delete(Resource.uri("components", part.get))

  def updateComponent(cmp: Component)
                     (implicit ec: ExecutionContext): HttpRequest =
    updateComponent(cmp.partNumber.get, cmp.description)

  def updateComponent(partNumber: String, description: String)
                     (implicit ec: ExecutionContext): HttpRequest =
    Put(Resource.uri("components", partNumber), Component.DescriptionWrapper(description))

}

trait ComponentRequests extends
    ComponentRequestsHttp with
    Matchers { self: ScalatestRouteTest =>

  def addComponentOK(part: Component.PartNumber, desc: String)
                    (implicit route: Route): Unit =
    addComponent(part, desc) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

}

/**
 * Testing Trait for building Filter requests
 */
trait FilterRequestsHttp extends Namespaces {

  def addFilter(name: String, expr: String)
               (implicit ec: ExecutionContext): HttpRequest =
    addFilter2(Filter(defaultNs, Refined.unsafeApply(name), Refined.unsafeApply(expr)))

  def addFilter2(filt: Filter)
                (implicit ec: ExecutionContext): HttpRequest =
    Post(Resource.uri("filters"), filt)

  def updateFilter(filt: Filter)
                  (implicit ec: ExecutionContext): HttpRequest =
    updateFilter(filt.name.get, filt.expression.get)

  def updateFilter(name: String, expr: String)
                  (implicit ec: ExecutionContext): HttpRequest =
    Put(Resource.uri("filters", name), Filter.ExpressionWrapper(Refined.unsafeApply(expr)))

  def listFilters: HttpRequest =
    Get(Resource.uri("filters"))

  def deleteFilter(filt: Filter): HttpRequest =
    deleteFilter(filt.name.get)

  def deleteFilter(name: String): HttpRequest =
    Delete(Resource.uri("filters", name))

  def listFiltersRegex(re: String): HttpRequest =
    Get(Resource.uri("filters") + "?regex=" + re)

  def validateFilter(filter: Filter)
                    (implicit ec: ExecutionContext): HttpRequest =
    Post(Resource.uri("validate", "filter"), filter)

}

trait FilterRequests extends FilterRequestsHttp with Matchers { self: ScalatestRouteTest =>

  def addFilterOK(name: String, expr: String)(implicit route: Route): Unit = {

    implicit val routeTimeout: RouteTestTimeout = RouteTestTimeout(5.second)

    addFilter(name, expr) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Filter] shouldBe Filter(defaultNs, Refined.unsafeApply(name), Refined.unsafeApply(expr))
    }
  }

  def updateFilterOK(name: String, expr: String)(implicit route: Route): Unit =
    updateFilter(name, expr) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Filter] shouldBe Filter(defaultNs, Refined.unsafeApply(name), Refined.unsafeApply(expr))
    }

  def deleteFilterOK(name: String)(implicit route: Route): Unit =
    deleteFilter(name) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

}

/**
 * Testing Trait for building PackageFilter requests
 */
trait PackageFilterRequestsHttp {

  def addPackageFilter2(pf: PackageFilter): HttpRequest = {
    Put(Resource.uri("packages", pf.packageName.get, pf.packageVersion.get, "filter", pf.filterName.get))
  }

  def addPackageFilter(pname: String, pversion: String, fname: String): HttpRequest =
    Put(Resource.uri("packages", pname, pversion, "filter", fname))

  def listPackageFilters: HttpRequest =
    Get(Resource.uri("packages", "filter"))

  def listPackagesForFilter(flt: Filter): HttpRequest =
    listPackagesForFilter(flt.name.get)

  def listPackagesForFilter(fname: String): HttpRequest =
    Get(Resource.uri("filters", fname, "package"))

  def listFiltersForPackage(pak: Package): HttpRequest =
    listFiltersForPackage(pak.id.name.get, pak.id.version.get)

  def listFiltersForPackage(pname: String, pversion: String): HttpRequest =
    Get(Resource.uri("packages", pname, pversion, "filter"))

  def deletePackageFilter(pkg: Package, filt: Filter): HttpRequest =
    deletePackageFilter(pkg.id.name.get, pkg.id.version.get, filt.name.get)

  def deletePackageFilter(pname: String, pversion: String, fname: String): HttpRequest =
    Delete(Resource.uri("packages", pname, pversion, "filter", fname))

}

trait PackageFilterRequests extends
  PackageFilterRequestsHttp with
  Matchers with
  Namespaces { self: ScalatestRouteTest =>

  def addPackageFilterOK(pname: String, pversion: String, fname: String)(implicit route: Route): Unit =
    addPackageFilter(pname, pversion, fname) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[PackageFilter] shouldBe
        PackageFilter(defaultNs, Refined.unsafeApply(pname), Refined.unsafeApply(pversion), Refined.unsafeApply(fname))
    }

  def deletePackageFilterOK(pname: String, pversion: String, fname: String)(implicit route: Route): Unit =
    deletePackageFilter(pname, pversion, fname) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
}

/**
 * Testing Trait for building Resolve requests
 */
trait ResolveRequestsHttp {

  def resolve2(namespace: Namespace, id: PackageId): HttpRequest =
    resolve(namespace, id.name.get, id.version.get)

  def resolve(pnamespace: Namespace, pname: String, pversion: String): HttpRequest =
    Get(Resource.uri("resolve").withQuery(Query(
      "namespace" -> pnamespace.get,
      "package_name" -> pname,
      "package_version" -> pversion
    )))
}

trait ResolveRequests extends
  ResolveRequestsHttp with
  Matchers with
  Namespaces { self: ScalatestRouteTest =>

  def resolveOK(pname: String, pversion: String, vins: Seq[Vehicle.Vin])(implicit route: Route): Unit = {

    resolve(defaultNs, pname, pversion) ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Map[Vehicle.Vin, List[PackageId]]] shouldBe
        ResolveFunctions.makeFakeDependencyMap(PackageId(Refined.unsafeApply(pname),
          Refined.unsafeApply(pversion)), vins)
    }
  }

}
