/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.core.image

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import eu.timepit.refined.api.Refined
import org.genivi.sota.core.db.image.DataType.{ImageId, ImageUpdate, ImageUpdateId}
import org.genivi.sota.core.{DatabaseSpec, DefaultPatience, ImageGenerators, LongRequestTimeout}
import org.genivi.sota.core.db.image.ImageRepositorySupport
import org.scalatest.{FunSuite, ShouldMatchers}
import org.scalatest.concurrent.ScalaFutures
import org.genivi.sota.http.NamespaceDirectives.defaultNamespaceExtractor
import io.circe.generic.auto._
import org.genivi.sota.core.data.UpdateSpec._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import StatusCodes._
import akka.http.scaladsl.server.Directives
import org.genivi.sota.data.DeviceGenerators
import cats.syntax.show.toShowOps
import org.genivi.sota.core.image.ImageUpdateResource.PendingImageUpdate
import org.genivi.sota.data.Namespaces.defaultNs

class ImageUpdateResourceSpec extends FunSuite
  with ScalatestRouteTest
  with DatabaseSpec
  with ShouldMatchers
  with ScalaFutures
  with ImageGenerators
  with DefaultPatience
  with ImageRepositorySupport
  with LongRequestTimeout
  with Directives {

  implicit val _db = db

  val route = new ImageUpdateResource(defaultNamespaceExtractor).route

  test("queuing fails when image does not exist") {
    val device = DeviceGenerators.genDevice.map(_.uuid).sample.get
    val imageId = ImageId(UUID.randomUUID())

    Post(s"/image_update/${imageId.get}/${device.show}") ~> route ~> check {
      status shouldBe PreconditionFailed
    }
  }

  test("GET returns empty list when no updates were pushed") {
    val device = DeviceGenerators.genDevice.map(_.uuid).sample.get

    Get(s"/image_update/${device.show}") ~> route ~> check {
      status shouldBe OK
      responseAs[List[ImageUpdate]] shouldBe empty
    }
  }

  test("POST queues an update") {
    val commit = commitGenerator.sample.get
    val refName = refNameGenerator.sample.get
    val device = DeviceGenerators.genDevice.map(_.uuid).sample.get

    val img = imageRepository
      .persist(defaultNs, commit, refName, "desc", Refined.unsafeApply("http//ats.com"))
      .futureValue

    Post(s"/image_update/${img.id.get}/${device.show}") ~> route ~> check {
      status shouldBe OK
      responseAs[ImageUpdateId] shouldBe a[ImageUpdateId]
    }
  }

  test("GET queue includes queued updates") {
    val commit = commitGenerator.sample.get
    val refName = refNameGenerator.sample.get
    val device = DeviceGenerators.genDevice.map(_.uuid).sample.get

    val img = imageRepository
      .persist(defaultNs, commit, refName, "desc", Refined.unsafeApply("http//ats.com"))
      .futureValue

    Post(s"/image_update/${img.id.get}/${device.show}") ~> route ~> check {
      status shouldBe OK
      responseAs[ImageUpdateId] shouldBe a[ImageUpdateId]
    }

    Get(s"/image_update/${device.show}") ~> route ~> check {
      status shouldBe OK

      val queue = responseAs[Seq[PendingImageUpdate]]

      queue.map(_.imageId) should contain(img.id)
      queue.map(_.commit) should contain(commit)
      queue.map(_.ref) should contain(refName)
    }
  }
}
