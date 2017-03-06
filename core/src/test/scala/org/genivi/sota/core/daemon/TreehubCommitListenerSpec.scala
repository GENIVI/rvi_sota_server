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
import scala.concurrent.ExecutionContext

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
    MessageDigest
      .getInstance("SHA-256")
      .digest(s.getBytes())
      .map { "%02x".format(_) }
      .foldLeft("") {_ + _}


  test("treehub commit event writes a package to the database") {
    import PackageId._

    implicit val connectivity = DefaultConnectivity
    val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)
    val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistry)
    val tufClient = FakeReposerverClient
    val publisher = LocalMessageBus.publisher(system)
    val listener = new TreehubCommitListener(db, updateService, tufClient, publisher)

    val ns = defaultNs
    val commit = toSHA256(UUID.randomUUID().toString)
    val event = TreehubCommit(
      ns = ns,
      commit = commit,
      refName = "some_ref_name",
      description = commit,
      size = 1234,
      uri = "some_uri"
    )
    val pid = PackageId(event.refName.refineTry[ValidName].get,
                        event.commit.refineTry[ValidVersion].get)

    val f = for {
      _   <- tufClient.createRoot(com.advancedtelematic.libats.data.Namespace(ns.get))
      _   <- listener.action(event)
      pkg <- db.run(Packages.find(event.ns, pid))
    } yield pkg

    f.futureValue shouldBe Package(ns, f.futureValue.uuid, pid, event.uri, event.size,
                                   commit, Some(event.description), None, None, f.futureValue.createdAt)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }

}
