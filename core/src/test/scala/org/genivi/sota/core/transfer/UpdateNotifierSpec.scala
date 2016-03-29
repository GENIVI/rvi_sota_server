package org.genivi.sota.core.transfer

import akka.event.Logging
import akka.http.scaladsl.model.Uri
import akka.testkit.TestKit
import eu.timepit.refined.api.Refined
import org.genivi.sota.core.PackagesReader
import org.genivi.sota.core.RequiresRvi
import org.genivi.sota.core.data.{UpdateRequest, UpdateSpec, UpdateStatus}
import org.genivi.sota.core.jsonrpc.HttpTransport
import org.genivi.sota.core.rvi.{SotaServices, RviConnectivity, RviUpdateNotifier}
import org.genivi.sota.data.Vehicle
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, PropSpec}
import scala.concurrent.Future
import slick.jdbc.JdbcBackend.Database


/**
 * Property-based spec for testing update notifier
 */
class UpdateNotifierSpec extends PropSpec with PropertyChecks with Matchers with BeforeAndAfterAll {
  import org.genivi.sota.core.Generators.{dependenciesGen, updateRequestGen, vinDepGen}

  val packages = scala.util.Random.shuffle( PackagesReader.read().take(100) )

  implicit val system = akka.actor.ActorSystem("UpdateServiseSpec")
  implicit val materilizer = akka.stream.ActorMaterializer()
  import system.dispatcher
  implicit val log = Logging(system, "org.genivi.sota.core.UpdateNotification")

  import org.scalatest.concurrent.ScalaFutures._

  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 20)

  implicit val connectivity = new RviConnectivity

  def updateSpecGen(vinGen : Gen[Vehicle.Vin]) : Gen[UpdateSpec] = for {
    updateRequest <- updateRequestGen(Gen.oneOf(packages).map( _.id) )
    vin           <- vinGen
    m             <- Gen.choose(1, 10)
    packages      <- Gen.pick(m, packages).map( _.toSet )
  } yield UpdateSpec( updateRequest, vin, UpdateStatus.Pending, packages )

  def updateSpecsGen( vinGen : Gen[Vehicle.Vin] ) : Gen[Seq[UpdateSpec]] =
    Gen.containerOf[Seq, UpdateSpec](updateSpecGen(vinGen))

  property("notify about available updates", RequiresRvi) {
    val serviceUri = Uri.from(scheme="http", host=getLocalHostAddr, port=8088)
    forAll( updateSpecsGen(Gen.const(Refined.unsafeApply("V1234567890123456"))) ) { specs =>
      val futureRes = for {
        sotaServices    <- SotaServices.register(serviceUri.withPath(Uri.Path / "rvi"))
        notifier         = new RviUpdateNotifier(sotaServices)
        notificationRes <- Future.sequence(notifier.notify(specs))
      } yield notificationRes
      futureRes.isReadyWithin( Span(5, Seconds) )  shouldBe true
    }
  }

  override def afterAll() : Unit = {
    TestKit.shutdownActorSystem(system)
  }

  def getLocalHostAddr = {
    import collection.JavaConversions._
    java.net.NetworkInterface.getNetworkInterfaces
      .flatMap(_.getInetAddresses.toSeq)
      .find(a => a.isSiteLocalAddress && !a.isLoopbackAddress)
      .getOrElse(java.net.InetAddress.getLocalHost)
      .getHostAddress
  }

}
