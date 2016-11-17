/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import cats.data.Xor
import org.genivi.sota.core.DatabaseSpec
import org.genivi.sota.data._
import org.genivi.sota.device_registry.db.DeviceRepository
import org.genivi.sota.http.AuthedNamespaceScope
import org.genivi.sota.http.UuidDirectives.{allowExtractor, extractUuid}
import org.genivi.sota.messaging.MessageBus
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec, Suite}

import scala.concurrent.Future
import scala.concurrent.duration._


trait ResourceSpec extends
         ScalatestRouteTest
    with BeforeAndAfterAll
    with DatabaseSpec
    with DeviceGenerators
    with DeviceRequests
    with GroupInfoGenerators
    with GroupRequests
    with Matchers
    with SimpleJsonGenerator
    with UuidGenerator {

  self: Suite =>

  implicit val _db = db

  implicit val routeTimeout: RouteTestTimeout =
    RouteTestTimeout(10.second)

  lazy val defaultNs: Namespace = Namespace("default")

  lazy val namespaceExtractor = Directives.provide(AuthedNamespaceScope(defaultNs))

  private val namespaceAuthorizer = allowExtractor(namespaceExtractor, extractUuid, deviceAllowed)

  private def deviceAllowed(deviceId: Uuid): Future[Namespace] = {
    db.run(DeviceRepository.deviceNamespace(deviceId))
  }

  lazy val messageBus =
    MessageBus.publisher(system, system.settings.config) match {
      case Xor.Right(v) => v
      case Xor.Left(err) => throw err
    }

  // Route
  lazy implicit val route: Route =
    new DeviceRegistryRoutes(namespaceExtractor, namespaceAuthorizer, messageBus).route
}

trait ResourcePropSpec extends PropSpec with ResourceSpec with PropertyChecks
