/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.implicits._
import io.circe.generic.auto._
import java.util.UUID

import eu.timepit.refined.api.Refined
import org.genivi.sota.DefaultPatience
import org.genivi.sota.core.daemon.DeltaListener
import org.genivi.sota.core.data.{Campaign, CampaignStatus}
import org.genivi.sota.core.db.{BlacklistedPackages, Packages, UpdateRequests}
import org.genivi.sota.core.resolver.DefaultConnectivity
import org.genivi.sota.core.transfer.DefaultUpdateNotifier
import org.genivi.sota.data._
import org.genivi.sota.http.NamespaceDirectives.defaultNamespaceExtractor
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.messaging.Commit.Commit
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.{DeltaGenerationFailed, GeneratedDelta}
import org.scalacheck.Gen
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

  val messageBus = MessageBusPublisher.ignore

  val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistry)
  val service =
    new CampaignResource(defaultNamespaceExtractor, deviceRegistry, updateService, messageBus)(db, system)

  object Resource {
    def uri(pathSuffixes: String*): Uri = {
      val BasePath = Path / "campaigns"
      Uri.Empty.withPath(pathSuffixes.foldLeft(BasePath)(_/_))
    }
  }

  def updateRequestMatch(urId: UUID, lc: LaunchCampaignRequest): Unit =
    whenReady(db.run(UpdateRequests.byId(urId))) { ur =>
      lc.startDate.foreach(ur.periodOfValidity.start shouldBe _)
      lc.endDate.foreach(ur.periodOfValidity.end shouldBe _)
      lc.priority.foreach(ur.priority shouldBe _)
      lc.signature.foreach(ur.signature shouldBe _)
      ur.description shouldBe lc.description
      lc.requestConfirmation.foreach(ur.requestConfirmation shouldBe _)
    }

  def updateRequestCancelled(urId: Uuid): Unit =
    whenReady(updateService.fetchUpdateSpecRows(urId)) { rows =>
      all(rows.map(_.status)) shouldBe UpdateStatus.Canceled
    }

  def cancel(id: Campaign.Id, expectedCode: StatusCode): Unit =
    Put(Resource.uri(id.show, "cancel")) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def blacklistPackage(pkgId: PackageId): Unit =
    BlacklistedPackages.create(Namespaces.defaultNs, pkgId, None).map(_ => ()).futureValue

  def createCampaign(name: CreateCampaign, expectedCode: StatusCode): Unit =
    Post(Resource.uri(), name) ~> service.route ~> check {
      status shouldBe expectedCode
    }

  def createCampaignOk(name: CreateCampaign): Campaign.Id =
    Post(Resource.uri(), name) ~> service.route ~> check {
      status shouldBe StatusCodes.Created
      responseAs[Campaign.Id]
    }

  def createCampaignWithStaticDeltaOk(launchCampaign: Boolean = true): Campaign = {
    val campName = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)
    val fromCommit = "c62ede9bdc7b53f7497e98af4381f95fde24667fc829aea8d933b70afedb7a0a"
    val toCommit = "c62ede9bdc7b53f7497e98af4381f95fde24667fc829aea8d933b70afedb7a0b"

    val pkgId = createRandomTreehubPackage(toCommit)
    setPackage(id, pkgId, StatusCodes.OK)

    val setgroups = SetCampaignGroupsGen.sample.get
    setGroups(id, setgroups, StatusCodes.OK)

    setDeltaFrom(id, Some(createRandomTreehubPackage(fromCommit)), StatusCodes.OK)

    if(launchCampaign) {
      val lc = LaunchCampaignGen.sample.get
      launch(id, lc, StatusCodes.NoContent)
    }

    fetchCampaignOk(id)
  }

  def createRandomPackage(): PackageId = {
    val pkg = PackageGen.sample.get

    whenReady(db.run(Packages.create(pkg))) { pkg =>
      pkg.id
    }
  }

  def createRandomTreehubPackage(hash: String): PackageId = {
    val pkg = PackageGen.sample.get
    val treehubPkg = pkg.copy(id = PackageId(pkg.id.name, Refined.unsafeApply(hash)))

    whenReady(db.run(Packages.create(treehubPkg))) { pkg =>
      pkg.id
    }
  }

  def createRandomGroups(): SetCampaignGroups = {

    val setgroups = SetCampaignGroupsGen.sample.get
    setgroups.groups.foreach { grpId =>
      val deviceUuids = Gen.nonEmptyContainerOf[List, Uuid](Uuid.generate()).sample.get
      deviceRegistry.addGroup(grpId, deviceUuids)
    }
    setgroups
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

  def launch(id: Campaign.Id, lc: LaunchCampaignRequest, expectedCode: StatusCode): Unit =
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

  def setDeltaFrom(id: Campaign.Id, pkg: Option[PackageId], expectedCode: StatusCode): Unit = {
    val query = pkg match {
      case Some(p) =>
        Query("deltaFromName" -> p.name.value, "deltaFromVersion" -> p.version.value)
      case None =>
        Query()
    }
    Put(Resource.uri(id.show, "delta").withQuery(query)) ~> service.route ~> check {
      status shouldBe expectedCode
    }
  }

  def launchCampaign(id: Campaign.Id): Campaign = {
    val pkgId = createRandomPackage()
    setPackage(id, pkgId, StatusCodes.OK)

    val setgroups = createRandomGroups()
    setGroups(id, setgroups, StatusCodes.OK)

    val lc = LaunchCampaignGen.sample.get
    launch(id, lc, StatusCodes.NoContent)

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

  test("can delete a static delta campaign") {
    val campaign = createCampaignWithStaticDeltaOk()
    removeCampaign(campaign.meta.id, StatusCodes.OK)
    val camps = getCampaignsOk()

    camps.map(_.id) shouldNot contain(campaign.meta.id)
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

    val setgroups = createRandomGroups()
    setGroups(id, setgroups, StatusCodes.Locked)

    val pkg = createRandomPackage()
    setPackage(id, pkg, StatusCodes.Locked)
  }

  test("can rename a launched campaign") {
    val campName = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)
    launchCampaign(id)

    val campName2 = CreateCampaignGen.sample.get
    renameCampaign(id, campName2, StatusCodes.OK)
  }

  test("can't launch campaign with inconsitent dates") {
    val campName = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)

    val pkgId = createRandomPackage()
    setPackage(id, pkgId, StatusCodes.OK)

    val setgroups = createRandomGroups()
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

    val setgroups = createRandomGroups()
    setGroups(id, setgroups, StatusCodes.OK)

    val lc = LaunchCampaignGen.sample.get
    launch(id, lc, StatusCodes.NoContent)
    launch(id, lc, StatusCodes.Locked)
  }

  test("can cancel a campaign") {
    val campName = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)

    val pkgId = createRandomPackage()
    setPackage(id, pkgId, StatusCodes.OK)

    val setgroups = createRandomGroups()
    setGroups(id, setgroups, StatusCodes.OK)

    val lc = LaunchCampaignGen.sample.get
    launch(id, lc, StatusCodes.NoContent)

    cancel(id, StatusCodes.OK)

    val camp = fetchCampaignOk(id)

    camp.groups.foreach { campGrp =>
      updateRequestCancelled(campGrp.updateRequest.get)
    }
  }

  test("campaign should not launch with blacklisted package") {
    val campName = CreateCampaignGen.sample.get
    val id = createCampaignOk(campName)

    val pkgId = createRandomPackage()
    setPackage(id, pkgId, StatusCodes.OK)

    val setgroups = SetCampaignGroupsGen.sample.get
    setGroups(id, setgroups, StatusCodes.OK)

    val lc = LaunchCampaignGen.sample.get

    blacklistPackage(pkgId)

    launch(id, lc, StatusCodes.BadRequest)

    val camp = fetchCampaignOk(id)

    camp.meta.status shouldBe CampaignStatus.Draft
  }

  test("campaign created date should not change on update") {
    val campName = CreateCampaignGen.sample.get

    val id = createCampaignOk(campName)

    val createdAt = getCampaignsOk().filter(_.id == id).map(_.createdAt)

    val pkgId = createRandomPackage()
    setPackage(id, pkgId, StatusCodes.OK)

    val setgroups = SetCampaignGroupsGen.sample.get
    setGroups(id, setgroups, StatusCodes.OK)

    getCampaignsOk().filter(_.id == id).map(_.createdAt) shouldEqual createdAt
  }

  test("campaign with static delta changes to 'in preparation' after launch") {
    createCampaignWithStaticDeltaOk().meta.status shouldBe CampaignStatus.InPreparation
  }

  test("campaign with static delta cannot be edited after launch") {
    val campaign = createCampaignWithStaticDeltaOk()

    setGroups(campaign.meta.id, createRandomGroups(), StatusCodes.Locked)

    val fromCommit = "162ede9bdc7b53f7497e98af4381f95fde24667fc829aea8d933b70afedb7a0a"
    setDeltaFrom(campaign.meta.id, Some(createRandomTreehubPackage(fromCommit)), StatusCodes.Locked)
  }

  test("campaign with static delta changes state to active after delta generation succeeds") {
    val campaign = createCampaignWithStaticDeltaOk()

    val fromCommit: Commit = Refined.unsafeApply("c62ede9bdc7b53f7497e98af4381f95fde24667fc829aea8d933b70afedb7a0a")
    val toCommit: Commit = Refined.unsafeApply("c62ede9bdc7b53f7497e98af4381f95fde24667fc829aea8d933b70afedb7a0b")
    val size = 100
    val msg = GeneratedDelta(campaign.meta.id.underlying, Namespaces.defaultNs, fromCommit, toCommit, Uri(), size)
    new DeltaListener(deviceRegistry, updateService, messageBus).generatedDeltaAction(msg).futureValue
    val camp = fetchCampaignOk(campaign.meta.id)
    camp.meta.status shouldBe CampaignStatus.Active
    camp.meta.size shouldEqual Some(size)
  }

  test("campaign with static delta changes state to draft after delta generation fails") {
    val campaign = createCampaignWithStaticDeltaOk()

    val failedMsg = DeltaGenerationFailed(campaign.meta.id.underlying, Namespaces.defaultNs, None)
    new DeltaListener(deviceRegistry, updateService, messageBus).deltaGenerationFailedAction(failedMsg).futureValue
    val camp = fetchCampaignOk(campaign.meta.id)
    camp.meta.status shouldBe CampaignStatus.Draft
    camp.meta.size shouldBe None
  }

  test("trying to create a static delta from the same commit fails") {
    val campaign = createCampaignWithStaticDeltaOk(false)

    setDeltaFrom(campaign.meta.id, Some(createRandomTreehubPackage(campaign.packageId.get.version.value)),
                 StatusCodes.BadRequest)
  }

}
