/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.daemon

import java.util.concurrent.CompletionStage

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.{CommittableMessage, CommittableOffset, PartitionOffset}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.genivi.sota.core.DatabaseSpec
import org.genivi.sota.messaging.Messages.PackageCreated
import org.genivi.sota.resolver.db.PackageRepository
import org.genivi.sota.resolver.test.DefaultPatience
import org.genivi.sota.resolver.test.generators.PackageGenerators
import org.scalacheck.Arbitrary
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{PropSpecLike, ShouldMatchers}

import scala.concurrent.Future



class PackageCreatedListenerSpec extends TestKit(ActorSystem("PackageCreatedListenerSpec"))
with PropSpecLike
with PropertyChecks
with ShouldMatchers
with DatabaseSpec
with PackageGenerators
with ScalaFutures
with Eventually
with DefaultPatience {

  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  implicit val genPkgCreated = genPackage.flatMap { p =>
    PackageCreated(p.namespace, p.id, p.description, p.vendor, None)
  }

  implicit val aGenPkgCreated = Arbitrary(genPkgCreated)

  class FakeCommitableOffset extends CommittableOffset {
    override def partitionOffset: PartitionOffset = throw new Exception("[test] partitionOffset not implemented")
    override def commitScaladsl(): Future[Done] = Future.successful(Done)
    override def commitJavadsl(): CompletionStage[Done] = throw new Exception("[test] commitJavadsl not implemented")
  }

  def fakeKafkaSource(packageCreated: PackageCreated) = {
    val record = new ConsumerRecord[Array[Byte], PackageCreated]("PackageCreated-test", 0, 0,
      Array.empty[Byte], packageCreated)

    val fakeMessage = new CommittableMessage[Array[Byte], PackageCreated] (record, new FakeCommitableOffset)

    Source.single[CommittableMessage[Array[Byte], PackageCreated]](fakeMessage)
  }


  property("saves package to database") {
    forAll { (pkg: PackageCreated) =>
      val source = PackageCreatedListener.buildSource(db, fakeKafkaSource(pkg))
      val sink = TestSink.probe[PackageCreated]

      val subscriber = source.runWith(sink)

      subscriber.requestNext()

      val dbIO = PackageRepository.exists(pkg.namespace, pkg.packageId)

      eventually {
        db.run(dbIO).futureValue.id shouldBe pkg.packageId
      }
    }
  }
}
