/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.storage

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import eu.timepit.refined.api.Refined
import org.genivi.sota.DefaultPatience
import org.genivi.sota.core.{Generators, IntegrationTest}
import org.genivi.sota.data.PackageId
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{FunSuiteLike, ShouldMatchers}

import scala.collection.JavaConversions._


class PackageStorageSpec extends TestKit(ActorSystem("PackageStorageSpec"))
  with FunSuiteLike
  with ShouldMatchers
  with PatienceConfiguration
  with DefaultPatience
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

  test("stores to s3 if credentials are defined", IntegrationTest) {
    val fileData = ByteString("""Não queiras, Lídia, edificar no espaço
                                |Que figuras futuro, ou prometer-te
                                |Amanhã. Cumpre-te hoje, não esperando.
                                |Tu mesma és tua vida.
                                |Não te destines, que não és futura.
                                |Quem sabe se, entre a taça que esvazias,
                                |E ela de novo enchida, não te a sorte
                                |Interpõe o abismo?""".stripMargin)

    val storage = new PackageStorage()(system, mat, ConfigFactory.load())

    val pkgId = PackageId(Refined.unsafeApply("somepkg"), Refined.unsafeApply("1.0.0"))

    val f = storage.store(pkgId, "fake-prefix", Source.single(fileData))

    whenReady(f) { case (uri, _, _) =>
      uri.toString() should
        endWith(".amazonaws.com/F4X6VFIA-fa7c205c024c75c292b8b2481e908e9c10523d8f")
    }
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
