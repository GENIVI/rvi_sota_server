/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.core.image

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.core.db.image.DataType.{Commit, Image}
import org.genivi.sota.core.db.image.ImageRepositorySupport
import org.genivi.sota.core.image.ImageResource.ImageRequest
import org.genivi.sota.core.{DatabaseSpec, DefaultPatience, ImageGenerators}
import org.genivi.sota.http.NamespaceDirectives.defaultNamespaceExtractor
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}
import org.genivi.sota.data.GeneratorOps.GenSample

class ImageResourceSpec extends FunSuite
  with ScalatestRouteTest
  with DatabaseSpec
  with ShouldMatchers
  with ScalaFutures
  with ImageGenerators
  with DefaultPatience
  with ImageRepositorySupport {

  implicit val _db = db

  val route = new ImageResource(defaultNamespaceExtractor).route

  test("POST Creates a new image") {
    val commit: Commit = commitGenerator.generate
    val refName = refNameGenerator.generate
    val req = ImageRequest(commit, refName, "desc", Refined.unsafeApply("http://ats.com"))

    Post("/image", req) ~> route ~> check {
      status shouldBe StatusCodes.OK

      val img = responseAs[Image]
      img.commit shouldBe commit
    }

    Get("/image") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[Image]].map(_.commit) should contain(commit)
    }
  }

  test("GET Retrieves a namespace image") {
    val imgG = imageGenerator.generate

    imageRepository.persist(imgG.namespace, imgG.commit, imgG.ref, imgG.description, imgG.pullUri).futureValue

    Get("/image") ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[Image]].map(_.commit) should contain(imgG.commit)
    }
  }
}
