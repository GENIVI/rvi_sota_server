/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.core

import java.io.File
import java.net.URI

import org.genivi.sota.marshalling.CirceMarshallingSupport._
import io.circe.generic.auto._
import org.genivi.sota.core.data.{Package => DataPackage}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import io.circe.generic.auto._
import org.genivi.sota.core.storage.PackageStorage.PackageStorageOp
import org.genivi.sota.core.storage.{LocalPackageStore, PackageStorage}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}

import scala.concurrent.Future

class PackagesResourceSpec extends FunSuite
  with ScalatestRouteTest
  with DatabaseSpec
  with ShouldMatchers
  with ScalaFutures
{
  import org.genivi.sota.http.NamespaceDirectives._

  val resolver = new FakeExternalResolver()

  val service = new PackagesResource(resolver, db, defaultNamespaceExtractor) {
    override val packageStorageOp: PackageStorageOp = new LocalPackageStore().store _
  }

  val BasePath = Path("/packages")

  val entity = HttpEntity(ByteString("Some Text"))

  val multipartForm =
    Multipart.FormData(Multipart.FormData.BodyPart.Strict(
      "file",
      entity,
      Map("filename" -> "linux-lts.rpm")))

  def readFile(uri: Uri): Future[ByteString] = {
    FileIO.fromPath(new File(new URI(uri.toString())).toPath)
      .runFold(ByteString.empty)(_ ++ _)
  }

  test("save packet to local file system") {
    val url = Uri.Empty.withPath(BasePath / "linux-lts" / "4.5.0")

    Put(url, multipartForm) ~> service.route ~> check {
      status shouldBe StatusCodes.NoContent

      Get("/packages") ~> service.route ~> check {
        val dataPackage = responseAs[List[DataPackage]].headOption
        dataPackage.map(_.id.name.get) should contain("linux-lts")

        whenReady(readFile(dataPackage.get.uri)) { contents =>
          contents shouldBe ByteString("Some Text")
        }
      }
    }
  }
}
