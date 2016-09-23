/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.syntax.show._
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import java.io.File
import org.genivi.sota.core.data.Campaign
import org.genivi.sota.core.db.Packages
import org.genivi.sota.data.PackageId
import org.genivi.sota.http.NamespaceDirectives.defaultNamespaceExtractor
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.scalacheck.Arbitrary
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}

class CampaignResourceSpec extends FunSuite
    with ScalatestRouteTest
    with DatabaseSpec
    with ShouldMatchers
    with ScalaFutures
    with LongRequestTimeout
    with DefaultPatience
    with Generators
{
  import Campaign._

  val service = new CampaignResource(db, defaultNamespaceExtractor)

  object Resource {
    def uri(pathSuffixes: String*): Uri = {
      val BasePath = Path / "campaigns"
      Uri.Empty.withPath(pathSuffixes.foldLeft(BasePath)(_/_))
    }
  }

  def createCampaign(name: CreateCampaign, expectedCode: StatusCode): Unit =
    Post(Resource.uri(), name) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def createCampaignOk(name: CreateCampaign): Id =
    Post(Resource.uri(), name) ~> service.route ~> check {
      status shouldBe StatusCodes.Created
      responseAs[Id]
    }

  def createRandomPackage(): PackageId = {
    val pkg = PackageGen.sample.get

    whenReady(db.run(Packages.create(pkg))) { pkg =>
      pkg.id
    }
  }

  def getCampaignsOk(): Seq[CampaignMeta] =
    Get(Resource.uri()) ~> service.route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[CampaignMeta]]
    }

  def launch(id: Id, expectedCode: StatusCode): Unit =
    Post(Resource.uri(id.show, "launch")) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def removeCampaign(id: Id, expectedCode: StatusCode): Unit =
    Delete(Resource.uri(id.show)) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def renameCampaign(id: Id, newName: CreateCampaign, expectedCode: StatusCode): Unit =
    Put(Resource.uri(id.show, "name"), newName) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def setGroups(id: Id, groups: CampaignGroups, expectedCode: StatusCode): Unit =
    Put(Resource.uri(id.show, "groups"),groups) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def setPackage(id: Id, pkg: PackageId, expectedCode: StatusCode): Unit =
    Put(Resource.uri(id.show, "package"), pkg) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def launchCampaign(id: Id): Unit = {
    val pkgId = createRandomPackage()
    setPackage(id, pkgId, StatusCodes.OK)

    val groups = CampaignGroupsGen.sample.get
    setGroups(id, groups, StatusCodes.OK)

    launch(id, StatusCodes.OK)
  }

  test("launch a campaign") {
    val campName = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)
    val camps = getCampaignsOk()

    camps.map(_.id) should contain(id)

    launchCampaign(id)
  }

  test("can't create two campaigns with the same name") {
    val campName = CreateCampaignGen.sample.get
    createCampaign(campName, StatusCodes.Created)
    createCampaign(campName, StatusCodes.Conflict)
  }

  test("can delete a campaign") {
    val campName = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)
    removeCampaign(id, StatusCodes.OK)
    val camps = getCampaignsOk()

    camps.map(_.id) shouldNot contain(id)

  }

  test("can rename campaign") {
    val campName = CreateCampaignGen.sample.get
    val campName2 = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)
    renameCampaign(id, campName2, StatusCodes.OK)
    renameCampaign(id, campName, StatusCodes.OK)
  }

  test("can't change name to an existing name") {
    val campName = CreateCampaignGen.sample.get
    val campName2 = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)
    val id2 = createCampaignOk(campName2)
    renameCampaign(id, campName2, StatusCodes.Conflict)
  }

  test("can't update launched campaign") {
    val campName = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)
    launchCampaign(id)

    val campName2 = CreateCampaignGen.sample.get
    renameCampaign(id, campName2, StatusCodes.Locked)

    val pkg = createRandomPackage()
    setPackage(id, pkg, StatusCodes.Locked)
  }
}
