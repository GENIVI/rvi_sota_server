/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.genivi.sota.core.{DatabaseSpec, FakeDeviceRegistry}
import org.genivi.sota.data.Device.DeviceName
import org.genivi.sota.data.{Device, Namespaces}
import org.genivi.sota.http.NamespaceDirectives
import org.genivi.sota.resolver.Routing
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, PropSpec, Suite, WordSpec}
import Device._
import cats.syntax.show._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._

trait ResourceSpec extends
  LongRequestTimeout
    with VehicleRequests
    with PackageRequests
    with FirmwareRequests
    with ComponentRequests
    with FilterRequests
    with PackageFilterRequests
    with ResolveRequests
    with ScalatestRouteTest
    with DatabaseSpec
    with BeforeAndAfterAll { self: Suite =>

  implicit val _db = db

  val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)

  import akka.http.scaladsl.server.Directives._

  // Route
  lazy implicit val route: Route = new Routing(NamespaceDirectives.defaultNamespaceExtractor,
    deviceRegistry).route ~ new FakeDeviceRegistryRoutes(deviceRegistry).route
}

/**
 * Generic trait for REST Word Specs
 * Includes helpers for Packages, Components, Filters, PackageFilters and
 * Resolver
 */
trait ResourceWordSpec extends WordSpec with ResourceSpec

/**
 * Generic trait for REST Property specs
 * Includes helpers for Packages, Components, Filters, PackageFilters and
 * Resolver
 */
trait ResourcePropSpec extends PropSpec with ResourceSpec with PropertyChecks

class FakeDeviceRegistryRoutes(deviceRegistry: FakeDeviceRegistry) {
  import akka.http.scaladsl.server.Directives._

  val route = path("fake_devices") {
    (put & entity(as[Device.Id])) { device =>
      deviceRegistry.addDevice(Device(Namespaces.defaultNs, device, DeviceName(s"name-${device.show}"), Option(DeviceId(device.show))))
      complete(StatusCodes.OK -> "")
    } ~
    get {
      complete(StatusCodes.MethodNotAllowed -> "")
    }
  }
}
