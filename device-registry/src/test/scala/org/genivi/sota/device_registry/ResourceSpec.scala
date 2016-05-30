/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.test

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.genivi.sota.core.DatabaseSpec
import org.genivi.sota.device_registry.Routing
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Suite, WordSpec, PropSpec, Matchers}
import org.scalatest.Matchers


trait ResourceSpec extends
         DeviceRequests
    with Matchers
    with ScalatestRouteTest
    with DatabaseSpec
    with BeforeAndAfterAll { self: Suite =>

  implicit val _db = db

  // Route
  lazy implicit val route: Route = new Routing().route

}

trait ResourcePropSpec extends PropSpec with ResourceSpec with PropertyChecks
