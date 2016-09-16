/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.daemon

import akka.actor.{ActorNotFound, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.testkit.scaladsl.TestSource
import akka.testkit.TestKit
import org.genivi.sota.core.DatabaseSpec
import org.genivi.sota.messaging.Messages.PackageCreated
import org.genivi.sota.resolver.db.PackageRepository
import org.genivi.sota.resolver.test.DefaultPatience
import org.genivi.sota.resolver.test.generators.PackageGenerators
import org.scalacheck.Arbitrary
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{PropSpecLike, ShouldMatchers}

import scala.concurrent.duration._

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

  property("saves package to database") {
    forAll { (pkg: PackageCreated) =>
      val sourceT = TestSource.probe[PackageCreated]

      val sink = Sink.actorSubscriber(Props(new PackageCreatedListener(db)))

      val publisher = sourceT.to(sink).run()

      publisher.sendNext(pkg)

      val dbIO = PackageRepository.exists(pkg.namespace, pkg.packageId)

      eventually {
        db.run(dbIO).futureValue.id shouldBe pkg.packageId
      }
    }
  }

  property("stops after stream is completed") {
    val sourceT = Source.empty[PackageCreated]

    val sink = Sink.actorSubscriber(Props(new PackageCreatedListener(db)))

    val publisher = sourceT.runWith(sink)

    val f =
      system.actorSelection(publisher.path)
        .resolveOne(5.seconds)
        .failed
        .futureValue

    f shouldBe a[ActorNotFound]
  }
}
