/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.storage

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import akka.testkit.TestKit
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.genivi.sota.core.IntegrationTest
import org.genivi.sota.data.PackageIdGenerators
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSuiteLike, ShouldMatchers}

import scala.concurrent.Future

class S3PackageStoreSpec extends TestKit(ActorSystem("LocalPackageStoreSpec"))
  with FunSuiteLike
  with ShouldMatchers
  with ScalaFutures {

  implicit val mat = ActorMaterializer()

  implicit val patience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val entity = HttpEntity(ByteString("""Não queiras, Lídia, edificar no espaço
                                       |Que figuras futuro, ou prometer-te
                                       |Amanhã. Cumpre-te hoje, não esperando.
                                       |Tu mesma és tua vida.
                                       |Não te destines, que não és futura.
                                       |Quem sabe se, entre a taça que esvazias,
                                       |E ela de novo enchida, não te a sorte
                                       |Interpõe o abismo?""".stripMargin))

  lazy val fileData = StrictForm.FileData(Some("filename.rpm"), entity)

  lazy val credentials = S3PackageStore.loadCredentials(system.settings.config).get
  lazy val s3 = new S3PackageStore(credentials)

  implicit val ec = system.dispatcher

  test("stores a file on s3 with correct credentials", IntegrationTest) {
    val packageId = PackageIdGenerators.genPackageId.sample.get

    val f = s3.store(packageId, fileData.filename.get, fileData.entity.dataBytes)

    whenReady(f) { case (uri, _, _) =>
      uri.toString should startWith("https://sota-core-it-tests.s3.eu-central-1.amazonaws.com/")
    }
  }

  test("returns a public URL given a package URI", IntegrationTest) {
    val packageId = PackageIdGenerators.genPackageId.sample.get

    def http(uri: Uri): Future[ByteString] = {
      Http()
        .singleRequest(HttpRequest(uri = uri))
        .flatMap(_.entity.dataBytes.runFold(ByteString(""))(_ ++ _))
    }

    val f = for {
      (uri, _, _) <- s3.store(packageId, fileData.filename.get, fileData.entity.dataBytes)
      (s3Url, _) <- s3.retrieve(packageId, uri)
      downloadContents <- http(s3Url)
    } yield downloadContents

    whenReady(f) { downloadContents =>
      downloadContents shouldBe fileData.entity.data
    }
  }

  test("retrieves a file from a temporary file", IntegrationTest) {
    val packageId = PackageIdGenerators.genPackageId.sample.get
    val f = for {
      (uri, _, _) <- s3.store(packageId, fileData.filename.get, fileData.entity.dataBytes)
      file <- s3.retrieveFile(uri)
      localContents <- FileIO.fromPath(file.toPath).runFold(ByteString.empty)(_ ++ _)
    } yield (file, localContents)

    whenReady(f) { case (file, localContents) =>
        file.exists() shouldBe true
        localContents shouldBe fileData.entity.data
    }
  }
}
