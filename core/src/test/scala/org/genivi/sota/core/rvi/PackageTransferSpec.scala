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
import com.typesafe.config.ConfigFactory
import io.circe.Encoder
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.security.MessageDigest

import org.apache.commons.codec.binary.Hex
import org.genivi.sota.core.Generators
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.resolver.ConnectivityClient
import java.time.Instant
import org.genivi.sota.data.Device
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}

import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

/**
 * Dummy actor to simulate SOTA Client in tests
 */
class ClientActor(device: Device.Id, probe: ActorRef, chunksToConsume: Int)
    extends Actor with ActorLogging with  Stash {

  val chunks = scala.collection.mutable.ListBuffer.empty[PackageChunk]

  def consumeChunks(uploader: ActorRef) : Receive = {
    case StartDownloadMessage(id, _, _) =>
      uploader ! ChunksReceived(device, id, List.empty)

    case PackageChunk(_, _, index) if index > chunksToConsume =>
      // do nothing, uploader will time out.

    case x @ PackageChunk(pid, data, index) =>
      chunks += x
      uploader ! ChunksReceived(device, pid, chunks.toList.map( _.index ) )

    case Finish =>
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

/**
 * Dummy actor to simulate SOTA Client in tests
 */
object ClientActor {

  final case class SetUploader( ref: ActorRef )
  final case class Report( chunks: List[PackageChunk] )

  def props( device: Device.Id, probe: ActorRef, chunksToConsume: Int = Int.MaxValue ) : Props =
    Props( new ClientActor( device, probe, chunksToConsume ) )

}

/**
 * Dummy actor to simulate RVI Client in tests
 */
class AccRviClient( clientActor: ActorRef ) extends ConnectivityClient {

  def sendMessage[A](service: String, message: A, expirationDate: Instant)
                 (implicit encoder: Encoder[A] ) : Future[Int] = {
    clientActor ! message
    FastFuture.successful( 1 )
  }

}

/**
 * Property-based spec for Package Transfers
 */
class PackageTransferSpec extends PropSpec with Matchers with PropertyChecks with BeforeAndAfterAll {

  import org.genivi.sota.data.DeviceGenerators._

  val config = ConfigFactory.parseString(
    """|akka {
       |  loglevel = debug
       |}
       |rvi {
       |  transfer {
       |    chunkSize = 1K
       |    ackTimeout = 300ms
       |  }
       |}
    """.stripMargin
  )

  implicit val system = akka.actor.ActorSystem( "test", Some(config) )
  val packages = org.genivi.sota.core.PackagesReader.read()

  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 1)

  val services = ClientServices("", "", "", "", "")

  ignore("all chunks transferred") {
    val testDataGen = for {
      device <- genDevice
      p <- Gen.oneOf(packages)
      updateId <- Gen.uuid
      signature <- Gen.alphaStr
    } yield (device, p, updateId, signature)

    forAll(testDataGen) { testData =>
      val (device, p, updateId, signature) = testData
      val pckg = Generators.generatePackageData(p)
      val probe = TestProbe()
      val clientActor = system.actorOf( ClientActor.props(device.id, probe.ref), "sota-client" )
      val rviClient = new AccRviClient( clientActor )
      val underTest = system.actorOf(PackageTransferActor.props(rviClient)(updateId, signature, pckg, services))
      clientActor ! ClientActor.SetUploader( underTest )
      val report = probe.expectMsgType[ClientActor.Report]
      val digest = MessageDigest.getInstance("SHA-1")
      report.chunks.sortBy(_.index).foreach(x => digest.update( x.bytes.toByteBuffer ) )
      pckg.checkSum shouldBe Hex.encodeHexString( digest.digest )
    }
  }

  ignore("transfer aborts after x attempts to deliver a chunk") {
    val chunkSize = system.settings.config.getBytes("rvi.transfer.chunkSize").intValue()
    val testDataGen = for {
      device    <- genDevice
      p         <- Gen.oneOf( packages.filter( _.size > chunkSize ) )
      updateId  <- Gen.uuid
      signature <- Gen.alphaStr
      chunks    <- Gen.choose(0,
                              (BigDecimal(p.size) / BigDecimal(chunkSize) setScale(0, RoundingMode.CEILING)).toInt - 1)
    } yield (device, p, updateId, signature, chunks)

    forAll( testDataGen ) { testData =>
      val (device, p, updateId, signature, chunksTransferred) = testData
      val pckg = Generators.generatePackageData(p)
      val probe = TestProbe()
      val clientActor = system.actorOf( ClientActor.props(device.id, probe.ref, chunksTransferred), "sota-client" )
      val rviClient = new AccRviClient( clientActor )
      val proxy = system.actorOf(
        Props(new Actor {
                val underTest = context.actorOf(PackageTransferActor.props(rviClient)(updateId, signature, pckg, services))
                def receive = {
                  case x if sender == underTest => probe.ref forward x
                  case x                        => underTest forward x
                }
              })
      )
      clientActor ! ClientActor.SetUploader( proxy )
      probe.expectMsg(UploadAborted)
    }
  }

  override def afterAll() : Unit = {
    akka.testkit.TestKit.shutdownActorSystem(system)
  }

}
