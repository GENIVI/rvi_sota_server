/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.time.Instant
import java.time.chrono.Chronology
import java.time.temporal.ChronoUnit

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import io.circe.generic.auto._
import org.genivi.sota.core.data.{DeviceStatus, DeviceUpdateStatus}
import org.genivi.sota.core.jsonrpc.HttpTransport
import org.genivi.sota.core.rvi._
import org.genivi.sota.data.{Device, DeviceGenerators, Namespaces, PackageIdGenerators}
import org.genivi.sota.datatype.NamespaceDirective
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

/**
 * Spec tests for vehicle REST actions
 */
class DeviceResourceSpec extends FunSuite
  with PropertyChecks
  with Matchers
  with ScalatestRouteTest
  with ScalaFutures
  with DatabaseSpec
  with UpdateResourcesDatabaseSpec
  with Namespaces {

  import CirceMarshallingSupport._
  import Generators._
  import PackageIdGenerators._
  import DeviceGenerators._
  import NamespaceDirective._

  implicit val routeTimeout: RouteTestTimeout =
    RouteTestTimeout(10.second)

  val rviUri = Uri(system.settings.config.getString( "rvi.endpoint" ))
  val serverTransport = HttpTransport( rviUri )
  implicit val rviClient = new JsonRpcRviClient( serverTransport.requestTransport, system.dispatcher)

  val fakeResolver = new FakeExternalResolver()
  val deviceRegistry = new FakeDeviceRegistry()

  lazy val service = new DevicesResource(db, rviClient, fakeResolver, deviceRegistry, defaultNamespaceExtractor)

  val BasePath = Path("/devices")

  implicit val patience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  implicit val _db = db

  def resourceUri( pathSuffix : String ) : Uri = {
    Uri.Empty.withPath(BasePath / pathSuffix)
  }

  def deviceUri(device: Device.Id)  = Uri.Empty.withPath( BasePath / device.underlying.get )

  test("returns vehicle status even if Vin is in device registry but not local db") {
    val device1 = genDeviceT.sample.get.copy(deviceId = Some(genDeviceId.sample.get))
    val device2 = genDeviceT.sample.get.copy(deviceId = Some(genDeviceId.sample.get))

    val f = for {
      id1 <- deviceRegistry.createDevice(device1)
      id2 <- deviceRegistry.createDevice(device2)
      _   <- db.run(createUpdateSpecFor(id2))
      _   <- deviceRegistry.updateLastSeen(id2, Instant.now.minus(1, ChronoUnit.HOURS))
    } yield (id1, id2)

    whenReady(f) { case(id1, id2) =>
      val url = Uri.Empty
        .withPath(BasePath)
        .withQuery(Uri.Query("status" -> "true"))

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val parsedResponse = responseAs[Seq[DeviceUpdateStatus]]

        parsedResponse should have size 2

        val foundDevice = parsedResponse.find(_.device == id1)

        foundDevice.flatMap(_.lastSeen) shouldNot be(defined)
        foundDevice.map(_.status) should contain(DeviceStatus.NotSeen)

        val foundDevice2 = parsedResponse.find(_.device == id2)

        foundDevice2.flatMap(_.lastSeen) should be(defined)
        foundDevice2.map(_.status) should contain(DeviceStatus.Outdated)
      }
    }
  }

  test("search with status=true returns current status for a device") {
    val device = genDeviceT.sample.get.copy(deviceId = Some(genDeviceId.sample.get))
    whenReady(deviceRegistry.createDevice(device)) { _ =>
      val url = Uri.Empty
        .withPath(BasePath)
        .withQuery(Uri.Query("status" -> "true"))

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val parsedResponse = responseAs[Seq[DeviceUpdateStatus]].headOption

        parsedResponse.flatMap(_.lastSeen) shouldNot be(defined)
        parsedResponse.map(_.status) should contain(DeviceStatus.NotSeen)
      }
    }
  }
}
