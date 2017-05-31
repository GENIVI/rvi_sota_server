/*
 * Copyright: Copyright (C) 2017, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.daemon

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.advancedtelematic.libats.data.RefinedUtils.RefineTry
import java.security.MessageDigest
import java.util.UUID

import org.genivi.sota.DefaultPatience
import org.genivi.sota.core._
import org.genivi.sota.core.client.FakeReposerverClient
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.Packages
import org.genivi.sota.core.resolver.DefaultConnectivity
import org.genivi.sota.core.transfer.DefaultUpdateNotifier
import org.genivi.sota.data._
import org.genivi.sota.messaging.LocalMessageBus
import org.genivi.sota.messaging.Messages.TreehubCommit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSuite, ShouldMatchers}

import scala.concurrent.{ExecutionContext, Future}
import PackageId._
import com.advancedtelematic.libtuf.data.TufDataType.RepoId

class TreehubCommitListenerSpec extends FunSuite
  with DatabaseSpec
  with ShouldMatchers
  with ScalaFutures
  with DefaultPatience
  with Namespaces
  with BeforeAndAfterAll {

  implicit val ec = ExecutionContext.global
  implicit val system = ActorSystem("TreehubCommitListenerSpec")
  implicit val mat = ActorMaterializer()

  def toSHA256(s: String) =
    MessageDigest.getInstance("SHA-256").digest(s.getBytes()).map { "%02x".format(_) }.foldLeft("") {_ + _}

  implicit val connectivity = DefaultConnectivity
  val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)
  val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistry)
  val tufClient = FakeReposerverClient
  val publisher = LocalMessageBus.publisher(system)
  val listener = new TreehubCommitListener(db, updateService, tufClient, publisher)

  def runListener(): Future[(RepoId, TreehubCommit, Package)] = {
    val commit = toSHA256(UUID.randomUUID().toString)

    val event = TreehubCommit(ns = defaultNs, commit = commit, refName = "some_ref_name", description = commit,
      size = 1234, uri = "some_uri")

    val pid = PackageId(event.refName.refineTry[ValidName].get, event.commit.refineTry[ValidVersion].get)

    for {
      repoId   <- tufClient.createRoot(com.advancedtelematic.libats.data.Namespace(defaultNs.get))
      _   <- listener.action(event)
      pkg <- db.run(Packages.find(event.ns, pid))
    } yield (repoId, event, pkg)
  }

  test("treehub commit event writes a package to the database") {
    val (_, event, dbPackage) = runListener().futureValue

    dbPackage.namespace shouldBe defaultNs
    dbPackage.uri.toString() shouldBe event.uri
    dbPackage.size shouldBe event.size
    dbPackage.checkSum shouldBe event.commit
    dbPackage.description shouldBe Some(event.description)
  }

  test("treehub commit event adds tuf target") {
    val (repoId, event, dbPackage) = runListener().futureValue

    val storedTarget = tufClient.targets(repoId).head

    storedTarget.namespace.get shouldBe defaultNs.get
    storedTarget.name.map(_.value) should contain(dbPackage.id.name.value)
    storedTarget.version.map(_.value) should contain(dbPackage.id.version.value)
    storedTarget.hardwareIds.map(_.value) should contain(event.refName)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }

}
