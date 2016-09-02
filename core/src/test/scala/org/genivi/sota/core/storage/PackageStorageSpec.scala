/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.storage

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.genivi.sota.core.Generators
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuiteLike, ShouldMatchers}

import scala.collection.JavaConversions._


class PackageStorageSpec extends TestKit(ActorSystem("PackageStorageSpec"))
  with FunSuiteLike
  with ShouldMatchers
  with ScalaFutures {

  implicit val mat = ActorMaterializer()
  implicit val config = system.settings.config

  val configWithLocalStorage = ConfigFactory
    .parseMap(Map("accessKey" -> "", "secretKey" -> "", "bucketId" -> ""))
    .atPath("core.s3")

  val localStorage = new PackageStorage()(system, mat, configWithLocalStorage).storage

  test("uses local file storage if no s3 credentials are available") {
    localStorage shouldBe a[LocalPackageStore]
  }

  test("uses s3 if if s3 credentials are defined") {
    val configWithS3 = ConfigFactory
      .parseMap(Map("accessKey" -> "fake", "secretKey" -> "fake", "bucketId" -> "bucket"))
      .atPath("core.s3")

    val storage = new PackageStorage()(system, mat, configWithS3).storage

    storage shouldBe a[S3PackageStore]
  }

  test("builds a 2xx response when using LocalStorage") {
    val packageModel =
      Generators.PackageGen.sample.get.copy(uri = Uri("file:///proc/cpuinfo"))

    val storage = new PackageStorage()(system, mat, configWithLocalStorage)

    val f = storage.retrieveResponse(packageModel)

    whenReady(f) { response =>
      response.status shouldBe StatusCodes.OK
    }
  }
}
