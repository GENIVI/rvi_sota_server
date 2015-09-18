package org.genivi.sota.core.rvi

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Stash
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.util.FastFuture
import akka.parboiled2.util.Base64
import akka.testkit.TestProbe
import io.circe.Encoder
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex
import org.genivi.sota.core.Generators
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.data.Vehicle
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Success

class ClientActor(vin: Vehicle.IdentificationNumber, probe: ActorRef) extends Actor with ActorLogging with  Stash {

  val chunks = scala.collection.mutable.ListBuffer.empty[PackageChunk]

  def consumeChunks(uploader: ActorRef) : Receive = {
    case StartDownloadMessage(pid, _, _) =>
      uploader ! ChunksReceived(vin, pid, List.empty)

    case x @ PackageChunk(pid, data, index) =>
      chunks += x
      uploader ! ChunksReceived(vin, pid, chunks.toList.map( _.index ) )

    case Finish(_) =>
      probe ! ClientActor.Report(chunks.toList)
      context stop self
  }

  override def receive = {
    case ClientActor.SetUploader( ref ) =>
      context.become( consumeChunks(ref) )
      unstashAll()

    case m =>
      stash()
  }

}

object ClientActor {

  final case class SetUploader( ref: ActorRef )
  final case class Report( chunks: List[PackageChunk] )

  def props( vin: Vehicle.IdentificationNumber, probe: ActorRef ) : Props = Props( new ClientActor( vin, probe ) )

}

class AccRviClient( clientActor: ActorRef ) extends RviClient {

  def sendMessage[A](service: String, message: A, expirationDate: DateTime)
                    (implicit encoder: Encoder[A] ) : Future[Int] = {
    clientActor ! message
    FastFuture.successful( 1 )
  }

}

class PackageTransferSpec extends PropSpec with Matchers with PropertyChecks with BeforeAndAfterAll {

  implicit val system = akka.actor.ActorSystem()
  val packages = org.genivi.sota.core.PackagesReader.read()

  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 1)

  property("all chunks transferred") {
    val services = ClientServices("", "", "")
    forAll( Generators.vehicleGen, Gen.oneOf( packages ) ) { (vehicle, p) =>
      val pckg = Generators.generatePackageData(p)
      val probe = TestProbe()
      val clientActor = system.actorOf( ClientActor.props(vehicle.vin, probe.ref), "sota-client" )
      val rviClient = new AccRviClient( clientActor )
      val underTest = system.actorOf( PackageTransferActor.props(rviClient)(services, pckg) )
      clientActor ! ClientActor.SetUploader( underTest )
      val report = probe.expectMsgType[ClientActor.Report]
      val digest = MessageDigest.getInstance("SHA-1")
      report.chunks.sortBy(_.index).foreach(x => digest.update( x.bytes.toByteBuffer ) )
      pckg.checkSum shouldBe Hex.encodeHexString( digest.digest )
    }
  }

  override def afterAll() : Unit = {
    akka.testkit.TestKit.shutdownActorSystem(system)
  }

}
