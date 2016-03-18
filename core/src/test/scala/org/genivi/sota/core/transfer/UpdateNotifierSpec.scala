package org.genivi.sota.core.transfer

import akka.event.Logging
import akka.http.scaladsl.model.Uri
import akka.testkit.TestKit
import eu.timepit.refined.api.Refined
import org.genivi.sota.core.PackagesReader
import org.genivi.sota.core.data.{UpdateRequest, UpdateSpec, UpdateStatus, Vehicle}
import org.genivi.sota.core.jsonrpc.HttpTransport
import org.genivi.sota.core.rvi.JsonRpcRviClient
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, PropSpec}
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}
import scala.concurrent.Future
import slick.jdbc.JdbcBackend.Database
import org.genivi.sota.core.RequiresRvi
import org.genivi.sota.core.rvi.SotaServices

object UpdateNotifierSpec {
  import org.genivi.sota.core.Generators.{dependenciesGen, updateRequestGen, vinDepGen}

  val packages = scala.util.Random.shuffle( PackagesReader.read().take(100) )

  def updateSpecGen(vinGen : Gen[Vehicle.Vin]) : Gen[UpdateSpec] = for {
    updateRequest <- updateRequestGen(Gen.oneOf(packages).map( _.id) )
    vin           <- vinGen
    m             <- Gen.choose(1, 10)
    packages      <- Gen.pick(m, packages).map( _.toSet )
  } yield UpdateSpec( updateRequest, vin, UpdateStatus.Pending, packages )

  def updateSpecsGen( vinGen : Gen[Vehicle.Vin] ) : Gen[Seq[UpdateSpec]] =
    Gen.containerOf[Seq, UpdateSpec](updateSpecGen(vinGen))
}

/**
 * Property-based spec for testing update notifier
 */
class UpdateNotifierSpec extends PropSpec with PropertyChecks with Matchers with BeforeAndAfterAll {
  import UpdateNotifierSpec._

  implicit val system = akka.actor.ActorSystem("UpdateServiseSpec")
  implicit val materilizer = akka.stream.ActorMaterializer()
  import system.dispatcher
  implicit val log = Logging(system, "org.genivi.sota.core.UpdateNotification")

  import org.scalatest.concurrent.ScalaFutures._

  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 20)

  val rviUri = Uri(system.settings.config.getString( "rvi.endpoint" ))
  val serverTransport = HttpTransport( rviUri )
  implicit val rviClient = new JsonRpcRviClient( serverTransport.requestTransport, system.dispatcher)

  property("notify about available updates", RequiresRvi) {
    import serverTransport.requestTransport
    val serviceUri = Uri.from(scheme="http", host=getLocalHostAddr, port=8088)
    forAll( updateSpecsGen(Gen.const(Refined.unsafeApply("V1234567890123456"))) ) { specs =>
      val futureRes = for {
        sotaServices    <- SotaServices.register( serviceUri.withPath(Uri.Path / "rvi") )
        notificationRes <- Future.sequence( UpdateNotifier.notify(specs, sotaServices) )
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
