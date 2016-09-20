/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.genivi.sota.core.db.{BlacklistedPackageRequest, BlacklistedPackageResponse, Packages}
import org.genivi.sota.data.PackageId
import org.scalatest.{FunSuite, ShouldMatchers}
import org.scalatest.concurrent.ScalaFutures
import org.genivi.sota.http.NamespaceDirectives._
import org.genivi.sota.messaging.MessageBusPublisher

class BlacklistResourceSpec extends FunSuite
  with ScalatestRouteTest
  with DatabaseSpec
  with ShouldMatchers
  with ScalaFutures
  with DefaultPatience
  with LongRequestTimeout
  with Generators {

  implicit val _db = db

  val serviceRoute = new BlacklistResource(defaultNamespaceExtractor, MessageBusPublisher.ignore).route

  private val blacklistPath = "/blacklist"

  def blacklistUrl(pkg: PackageId): Uri =
    Uri.Empty.withPath(Path("/blacklist") / pkg.name.get / pkg.version.get)

  def createBlacklist(): data.Package = {
    val pkg = PackageGen.sample.get
    db.run(Packages.create(pkg)).futureValue

    val blacklistReq = BlacklistedPackageRequest(pkg.id, Some("Some comment"))

    Post(blacklistPath, blacklistReq) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.Created
    }

    pkg
  }

  test("packages can be flagged as blacklist") {
    createBlacklist()
  }

  test("can create blacklist for a non existent package (foreign package)") {
    val pkg = PackageGen.sample.get

    val blacklistReq = BlacklistedPackageRequest(pkg.id, Some("Some comment"))

    Post(blacklistPath, blacklistReq) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.Created
    }
  }

  test("can return a list of blacklisted packages") {
    val pkg = createBlacklist()

    Get("/blacklist") ~> serviceRoute ~> check {
      status shouldBe StatusCodes.OK

      val resp = responseAs[Seq[BlacklistedPackageResponse]]

      resp.map(_.packageId) should contain(pkg.id)
    }

  }

  test("packages blacklist can be updated") {
    val pkg = createBlacklist()
    val blacklistReq = BlacklistedPackageRequest(pkg.id, Some("Hi"))

    Put(blacklistPath, blacklistReq) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.OK

      Get("/blacklist") ~> serviceRoute ~> check {
        val r = responseAs[Seq[BlacklistedPackageResponse]]

        r.find(_.packageId == pkg.id).map(_.comment) should contain("Hi")
      }
    }
  }

  test("updating a missing package returns NotFound") {
    val pkg = PackageGen.sample.get
    db.run(Packages.create(pkg)).futureValue

    val blacklistReq = BlacklistedPackageRequest(pkg.id, Some("Some comment"))

    Put(blacklistPath, blacklistReq.copy(comment = Some("Hi"))) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }

  test("packages can be unflagged as blacklisted") {
    val pkg = createBlacklist()
    val url = blacklistUrl(pkg.id)

    Delete(url) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.OK
    }

    Get("/blacklist") ~> serviceRoute ~> check {
      val r = responseAs[Seq[BlacklistedPackageResponse]]
      r.find(_.packageId == pkg.id) shouldBe empty
    }
  }

  test("can create the same blacklist twice") {
    val pkg = createBlacklist()
    val url = blacklistUrl(pkg.id)

    Delete(url) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.OK
    }

    val blacklistReq = BlacklistedPackageRequest(pkg.id, Some("Some comment"))

    Post(blacklistPath, blacklistReq) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.Created
    }

    Get("/blacklist") ~> serviceRoute ~> check {
      val r = responseAs[Seq[BlacklistedPackageResponse]]
      r.find(_.packageId == pkg.id) shouldNot be(empty)
    }
  }

  test("creating the same blacklist after DELETE works, updating the previous entry") {
    val pkg = createBlacklist()
    val url = blacklistUrl(pkg.id)

    Delete(url) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.OK
    }

    val blacklistReq = BlacklistedPackageRequest(pkg.id, Some("Some comment"))

    Post(url, blacklistReq) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.Created
    }

    Get("/blacklist") ~> serviceRoute ~> check {
      val r = responseAs[Seq[BlacklistedPackageResponse]]
      r.filter(_.packageId == pkg.id) should have size 1
    }
  }

  test("creating the same blacklist twice fails") {
    val pkg = createBlacklist()
    val url = blacklistUrl(pkg.id)

    val blacklistReq = BlacklistedPackageRequest(pkg.id, Some("Some comment"))

    Post(url, blacklistReq) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.Conflict
    }
  }


  test("cannot remove an already removed package") {
    val pkg = createBlacklist()
    val url = blacklistUrl(pkg.id)

    Delete(url) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.OK
    }

    Delete(url) ~> serviceRoute ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }
}
