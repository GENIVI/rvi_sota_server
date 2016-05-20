/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.genivi.sota.core.DatabaseSpec
import org.genivi.sota.resolver.Routing
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, PropSpec, Suite, WordSpec}

/**
 * Generic trait for REST specs
 * Includes helpers for Packages, Components, Filters, PackageFilters and
 * Resolver
 */
trait ResourceSpec extends
         VehicleRequests
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

  // Route
  lazy implicit val route: Route = new Routing().route
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
