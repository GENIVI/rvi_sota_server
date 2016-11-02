/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.syntax.show._
import io.circe.generic.auto._
import java.util.UUID
import org.genivi.sota.core.data.{Campaign, UpdateRequest}
import org.genivi.sota.core.db.{Packages, UpdateRequests}
import org.genivi.sota.core.resolver.DefaultConnectivity
import org.genivi.sota.core.transfer.DefaultUpdateNotifier
import org.genivi.sota.data.{Interval, Namespaces, PackageId}
import org.genivi.sota.http.NamespaceDirectives.defaultNamespaceExtractor
import org.genivi.sota.marshalling.CirceMarshallingSupport._
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

  implicit val connectivity = DefaultConnectivity

  val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)

  val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistry)
  val service = new CampaignResource(defaultNamespaceExtractor, deviceRegistry, updateService)(db, system)

  object Resource {
    def uri(pathSuffixes: String*): Uri = {
      val BasePath = Path / "campaigns"
      Uri.Empty.withPath(pathSuffixes.foldLeft(BasePath)(_/_))
    }
  }

  def updateRequestMatch(urId: UUID, lc: LaunchCampaign): Unit = {
    whenReady(db.run(UpdateRequests.byId(urId))) { ur =>
      lc.startDate.foreach(ur.periodOfValidity.start shouldBe _)
      lc.endDate.foreach(ur.periodOfValidity.end shouldBe _)
      lc.priority.foreach(ur.priority shouldBe _)
      lc.signature.foreach(ur.signature shouldBe _)
      ur.description shouldBe lc.description
      lc.requestConfirmation.foreach(ur.requestConfirmation shouldBe _)
    }

  }

  def createCampaign(name: CreateCampaign, expectedCode: StatusCode): Unit =
    Post(Resource.uri(), name) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def createCampaignOk(name: CreateCampaign): Campaign.Id =
    Post(Resource.uri(), name) ~> service.route ~> check {
      status shouldBe StatusCodes.Created
      responseAs[Campaign.Id]
    }

  def createRandomPackage(): PackageId = {
    val pkg = PackageGen.sample.get

    whenReady(db.run(Packages.create(pkg))) { pkg =>
      pkg.id
    }
  }

  def fetchCampaignOk(id: Campaign.Id): Campaign =
    Get(Resource.uri(id.show)) ~> service.route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Campaign]
    }

  def getCampaignsOk(): Seq[CampaignMeta] =
    Get(Resource.uri()) ~> service.route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Seq[CampaignMeta]]
    }

  def launch(id: Campaign.Id, lc: LaunchCampaign, expectedCode: StatusCode): Unit =
    Post(Resource.uri(id.show, "launch"), lc) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def removeCampaign(id: Campaign.Id, expectedCode: StatusCode): Unit =
    Delete(Resource.uri(id.show)) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def renameCampaign(id: Campaign.Id, newName: CreateCampaign, expectedCode: StatusCode): Unit =
    Put(Resource.uri(id.show, "name"), newName) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def setGroups(id: Campaign.Id, groups: SetCampaignGroups, expectedCode: StatusCode): Unit =
    Put(Resource.uri(id.show, "groups"),groups) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def setPackage(id: Campaign.Id, pkg: PackageId, expectedCode: StatusCode): Unit =
    Put(Resource.uri(id.show, "package"), pkg) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def launchCampaign(id: Campaign.Id): Campaign = {
    val pkgId = createRandomPackage()
    setPackage(id, pkgId, StatusCodes.OK)

    val setgroups = SetCampaignGroupsGen.sample.get
    setGroups(id, setgroups, StatusCodes.OK)

    val lc = LaunchCampaignGen.sample.get
    launch(id, lc, StatusCodes.OK)

    val camp = fetchCampaignOk(id)

    camp.packageId should not be None

    camp.groups.map(_.group).toSet shouldBe setgroups.groups.toSet

    camp.groups.foreach { campGrp =>
      campGrp.updateRequest should not be None
      updateRequestMatch(campGrp.updateRequest.get.toJava, lc)
    }

    camp
  }

  test("launch a campaign") {
    val campName = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)
    val camps = getCampaignsOk()

    camps.map(_.id) should contain(id)

    val camp = launchCampaign(id)

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

  test("can't launch campaign with inconsitent dates") {
    val campName = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)

    val pkgId = createRandomPackage()
    setPackage(id, pkgId, StatusCodes.OK)

    val setgroups = SetCampaignGroupsGen.sample.get
    setGroups(id, setgroups, StatusCodes.OK)

    val lc = LaunchCampaignGen.sample.get
    val inter = IntervalGen.sample.get
    launch(id, lc.copy(startDate = Some(inter.end), endDate = Some(inter.start)), StatusCodes.Conflict)
  }

  test("can't launch a campaign twice") {
    val campName = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)

    val pkgId = createRandomPackage()
    setPackage(id, pkgId, StatusCodes.OK)

    val setgroups = SetCampaignGroupsGen.sample.get
    setGroups(id, setgroups, StatusCodes.OK)

    val lc = LaunchCampaignGen.sample.get
    launch(id, lc, StatusCodes.OK)
    launch(id, lc, StatusCodes.Locked)
  }
}
