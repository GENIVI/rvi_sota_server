/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import io.circe.generic.auto._
import org.genivi.sota.core.data.InstallHistory
import org.genivi.sota.http.NamespaceDirectives.defaultNamespaceExtractor
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}
import cats.syntax.show._
import org.genivi.sota.DefaultPatience
import org.genivi.sota.data.DeviceGenerators.{genDeviceId, genDeviceT}
import org.genivi.sota.data.Namespaces


class HistoryResourceSpec() extends FunSuite
  with ScalatestRouteTest
  with DatabaseSpec
  with UpdateResourcesDatabaseSpec
  with ShouldMatchers
  with ScalaFutures
  with LongRequestTimeout
  with DefaultPatience
  with Generators {

  implicit val _db = db

  val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)
  val service = new HistoryResource(deviceRegistry, defaultNamespaceExtractor)
  val baseUri = Uri.Empty.withPath(Path("/history"))

  test("history") {
    val device = genDeviceT.sample.get.copy(deviceId = Some(genDeviceId.sample.get))

    whenReady(deviceRegistry.createDevice(device)) { case uuid =>
      val uri = Uri.Empty.withPath(baseUri.path).withQuery(Query("uuid" -> uuid.show))

      Get(baseUri.withQuery(Query("uuid" -> uuid.show))) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Seq[InstallHistory]] should be(empty)
      }
    }
  }
}
