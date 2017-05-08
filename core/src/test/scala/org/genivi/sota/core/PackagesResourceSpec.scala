/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
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
import akka.testkit.TestKitBase
import akka.util.ByteString
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.DefaultPatience
import org.genivi.sota.core.db.{BlacklistedPackages, Packages}
import org.genivi.sota.core.resolver.DefaultConnectivity
import org.genivi.sota.core.storage.PackageStorage.PackageStorageOp
import org.genivi.sota.core.storage.LocalPackageStore
import org.genivi.sota.core.transfer.DefaultUpdateNotifier
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}
import org.genivi.sota.data.{Namespace, Namespaces, PackageId}
import org.genivi.sota.messaging.LocalMessageBus
import org.genivi.sota.messaging.Messages.PackageStorageUsage

import scala.concurrent.duration._
import scala.concurrent.Future

class PackagesResourceSpec extends FunSuite
  with ScalatestRouteTest
  with DatabaseSpec
  with ShouldMatchers
  with ScalaFutures
  with LongRequestTimeout
  with DefaultPatience
  with Generators
  with TestKitBase {

  import org.genivi.sota.http.NamespaceDirectives._

  val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)

  implicit val connectivity = DefaultConnectivity

  lazy val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistry)
  val service = new PackagesResource(updateService, db, LocalMessageBus.publisher(system), defaultNamespaceExtractor) {
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
        dataPackage.map(_.id.name.value) should contain("linux-lts")

        whenReady(readFile(dataPackage.get.uri)) { contents =>
          contents shouldBe ByteString("Some Text")
        }
      }
    }
  }

  test("returns packages for the request namespace only") {
    val pkg = PackageGen.sample.get.copy(namespace = Namespace("not-the-default-ns"))
    val dbF = db.run(Packages.create(pkg))

    whenReady(dbF) { pkg =>
      Get("/packages") ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        val packages = responseAs[List[DataPackage]]

        packages.map(_.id) shouldNot contain(pkg.id)
      }
    }
  }

  test("returns package blacklist info when searching blacklisted package") {
    val pkg = PackageGen.sample.get
    val dbF = for {
      _ <- db.run(Packages.create(pkg))
      _ <- BlacklistedPackages.create(pkg.namespace, pkg.id)
    } yield pkg

    whenReady(dbF) { pkg =>
      Get("/packages") ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        val responseP = responseAs[List[Json]]
          .find { j =>
            j.hcursor.downField("id").as[PackageId] === Right(pkg.id)
          }
          .map { pp =>
            pp.hcursor.downField("isBlackListed").as[Boolean].right.get
          }

        responseP should contain(true)
      }
    }
  }

  test("returns package blacklist info when searching non blacklisted package") {
    val pkg = PackageGen.sample.get
    val dbF = db.run(Packages.create(pkg))

    whenReady(dbF) { pkg =>
      Get("/packages") ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        val responseP = responseAs[List[Json]]
          .find { j =>
            j.hcursor.downField("id").as[PackageId] === Right(pkg.id)
          }
          .map { pp =>
            pp.hcursor.downField("isBlackListed").as[Boolean].right.get
          }

        responseP should contain(false)
      }
    }
  }

  test("returns package blacklist info when returning a package") {
    val pkg = PackageGen.sample.get
    val dbF = for {
      _ <- db.run(Packages.create(pkg))
      _ <- BlacklistedPackages.create(pkg.namespace, pkg.id)
    } yield pkg

    whenReady(dbF) { pkg =>
      Get(s"/packages/${pkg.id.name.value}/${pkg.id.version.value}") ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        val responseP = responseAs[Json]
          .hcursor.downField("isBlackListed").as[Boolean].right.get

        responseP shouldBe true
      }
    }
  }

  test("publishes message to bus on create") {
    val url = Uri.Empty.withPath(BasePath / "linux-lts" / "4.5.0")

    system.eventStream.subscribe(testActor, classOf[PackageStorageUsage])

    Put(url, multipartForm) ~> service.route ~> check {
      status shouldBe StatusCodes.NoContent
      expectMsgPF(10.seconds, "package usage greater than 0") {
        case m @ PackageStorageUsage(_, _, usage) if usage > 0L => m
      }
    }
  }
}
