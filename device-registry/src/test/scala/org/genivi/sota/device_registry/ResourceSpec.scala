/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.test

import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.genivi.sota.core.DatabaseSpec
import org.genivi.sota.data.Namespace
import org.genivi.sota.device_registry.Routing
import org.genivi.sota.messaging.Messages.DeviceCreated
import org.genivi.sota.messaging.{MessageBusManager, MessageBusPublisher}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec, Suite}

import scala.concurrent.duration._


trait ResourceSpec extends
         DeviceRequests
    with Matchers
    with ScalatestRouteTest
    with DatabaseSpec
    with BeforeAndAfterAll { self: Suite =>

  import cats.data.Xor

  implicit val _db = db

  implicit val routeTimeout: RouteTestTimeout =
    RouteTestTimeout(10.second)

  lazy val defaultNs: Namespace = Namespace("default")

  lazy val namespaceExtractor = Directives.provide(defaultNs)

  lazy val messageBusPublish: MessageBusPublisher[DeviceCreated] =
    MessageBusManager
      .getDeviceCreatedPublisher(system, system.settings.config) match {
      case Xor.Right(v) => v
      case Xor.Left(err) => throw err
    }


  // Route
  lazy implicit val route: Route = new Routing(namespaceExtractor, messageBusPublish).route

}

trait ResourcePropSpec extends PropSpec with ResourceSpec with PropertyChecks
