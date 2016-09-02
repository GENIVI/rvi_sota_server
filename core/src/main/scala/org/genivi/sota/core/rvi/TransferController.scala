/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.rvi

import java.io.File

import akka.actor._
import akka.util.ByteString
import io.circe.Encoder
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Paths, StandardOpenOption}
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.genivi.sota.core.data.{Package, UpdateRequest, UpdateSpec, UpdateStatus}
import akka.actor._
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import org.apache.commons.codec.binary.Base64
import org.genivi.sota.core.db.UpdateSpecs
import org.genivi.sota.core.db.UpdateSpecs.MiniUpdateSpec
import org.genivi.sota.core.resolver.ConnectivityClient
import org.genivi.sota.core.storage.S3PackageStore
import java.time.Instant
import java.time.Duration

import org.genivi.sota.core.transfer.DeviceUpdates
import org.genivi.sota.data.Device
import org.genivi.sota.messaging.MessageBusPublisher

import scala.collection.immutable.Queue
import scala.concurrent.duration.FiniteDuration
import scala.math.BigDecimal.RoundingMode
import slick.driver.MySQLDriver.api.Database

import scala.collection.LinearSeq
import scala.util.Try


/**
 * Actor to handle events received from the RVI node.
 *
 * @param transferProtocolProps the configuration class for creating actors to handle a single vehicle
 * @see SotaServices
 */
class UpdateController(transferProtocolProps: Props) extends Actor with ActorLogging {

  /**
   * Create actors to handle new downloads to a single vehicle.
   * Forward [[ChunksReceived]] and [[InstallReport]] messages to that actor until it finishes the download.
   * Remove the actor from the map when it is terminated.
   *
   * @param currentDownloads the map of device to the actor handling that vehicle
   */
  def running( currentDownloads: Map[Device.Id, ActorRef] ) : Receive = {
    case x @ StartDownload(device, updateId, clientServices) =>
      currentDownloads.get(device) match {
        case None =>
          log.debug(s"New transfer to vehicle $device for update $updateId, count ${currentDownloads.size}")
          val actor = context.actorOf( transferProtocolProps )
          context.watch(actor)
          context.become( running( currentDownloads.updated( device, actor ) ) )
          actor ! x
        case Some(x) =>
          log.warning(
            s"There is an active transfer for vehicle $device. Request to transfer update $updateId will be ignored." )
      }
    case x @ ChunksReceived(device, _, _) =>
      currentDownloads.get(device).foreach( _ ! x)

    case x: InstallReport =>
      log.debug(s"Install report from vehicle ${x.device}")
      currentDownloads.get(x.device).foreach( _ ! x )

    case akka.actor.Terminated(x) =>
      log.debug(s"Transfer actor terminated, count ${currentDownloads.size}")
      context.become( running( currentDownloads.filterNot( _._2 == x ) ) )
  }

  /**
   * Entry point with initial empty map of downloads
   */
  override def receive : Receive = running( Map.empty )

}

object UpdateController {

  /**
   * Configuration for creating [[UpdateController]] actors.
   */
  def props( transferProtocolProps: Props ) : Props = Props( new UpdateController(transferProtocolProps) )

}

object TransferProtocolActor {

  /**
   * Configuration for creating [[TransferProtocolActor]] actors.
   */
  def props(db: Database,
            rviClient: ConnectivityClient,
            transferActorProps: (UUID, String, Package, ClientServices) => Props,
            messageBus: MessageBusPublisher): Props =
    Props( new TransferProtocolActor( db, rviClient, transferActorProps, messageBus) )
}

object UpdateEvents {

  /**
   * Message from [[TransferProtocolActor]] when all packages are transferred to vehicle.
   */
  final case class PackagesTransferred( update: MiniUpdateSpec )

  /**
   * Message from [[TransferProtocolActor]] when an installation report is received from the vehicle.
   */
  final case class InstallReportReceived( report: InstallReport )

}

/**
 * Actor to transfer packages to a single vehicle.
 *
 * @param db the database connection
 * @param rviClient the client to the RVI node
 * @param transferActorProps the configuration class for creating the PackageTransferActor
 */
class TransferProtocolActor(db: Database,
                            rviClient: ConnectivityClient,
                            transferActorProps: (UUID, String, Package, ClientServices) => Props,
                            messageBus: MessageBusPublisher)
    extends Actor with ActorLogging {
  import cats.syntax.eq._
  import cats.syntax.show._
  import context.dispatcher

  val installTimeout : FiniteDuration = FiniteDuration(
    context.system.settings.config.getDuration("rvi.transfer.installTimeout", TimeUnit.MILLISECONDS),
    TimeUnit.MILLISECONDS
  )

  // scalastyle:off cyclomatic.complexity
  // scalastyle:off method.length
  /**
   * Third and last [[TransferProtocolActor]] behavior.
   *
   * Create actors to handle each transfer to the vehicle.
   * Forward ChunksReceived messages from the vehicle to the transfer actors.
   * Terminate when all updates and dependencies are successfully transferred,
   * or when the transfer is aborted because the vehicle is not responding.
   */
  def running(services: ClientServices,
              updates: MiniUpdateSpec,
              pending: Queue[Package],
              inProgress: (ActorRef, Package),
              done: Set[Package]) : Receive = {

    case akka.actor.Terminated(ref) =>
      // TODO: Handle actors terminated on errors (e.g. file exception in PackageTransferActor)
      val (oldActor, oldPkg) = inProgress
      assert(oldActor == ref) // TODO debug code
      log.debug(s"Package for update $updates uploaded.")

      if (pending.isEmpty) {
        log.debug( s"All packages uploaded." )
        context.setReceiveTimeout(installTimeout)
        context.system.eventStream.publish( UpdateEvents.PackagesTransferred( updates ) )
      } else {
        val nextInProgress = startPackageUpload(services)(updates, pending.head)
        val finishedPackageTransfers = done + oldPkg
        context become running(services, updates, pending.tail, nextInProgress, finishedPackageTransfers)
      }

    case x @ ChunksReceived(_, updateId, _) =>
      assert(updates.requestId == updateId) // TODO debug code
      val (inprActor, inprPkg) = inProgress
      inprActor ! x

    case r @ InstallReport(device, update) =>
      context.system.eventStream.publish( UpdateEvents.InstallReportReceived(r) )
      log.debug(s"Install report received from $device: ${update.update_id} installed with ${update.operation_results}")
      assert(updates.requestId == update.update_id) // TODO debug code
      DeviceUpdates.reportInstall(device, update, messageBus)(dispatcher, db)
      rviClient.sendMessage(services.getpackages, io.circe.Json.Null, ttl())

    case ReceiveTimeout =>
      abortUpdate(services, updates)

    case UploadAborted =>
      abortUpdate(services, updates)
  }
  // scalastyle:on

  def abortUpdate (services: ClientServices, x: MiniUpdateSpec): Unit = {
    rviClient.sendMessage(services.abort, io.circe.Json.Null, ttl())
    db.run( UpdateSpecs.setStatus(x.device, x.requestId, UpdateStatus.Canceled) )
    context.stop(self)
  }

  def ttl() : Instant = {
    Instant.now.plus(Duration.ofMinutes(5))
  }

  /**
    * Create a worker [[TransferProtocolActor]] to upload the given package to the vehicle.
    */
  def startPackageUpload(services: ClientServices)
                        (update: MiniUpdateSpec,
                         pkg: Package): (ActorRef, Package) = {
    val ref = context.actorOf(
      transferActorProps(update.requestId, update.requestSignature, pkg, services), update.requestId.toString
    )
    context.watch(ref)
    (ref, pkg)
  }

  /**
    * Second [[TransferProtocolActor]] behavior, about to upload [[UpdateSpec]]-s to the vehicle.
    */
  def loadSpecs(services: ClientServices) : Receive = {
    case Some(spec: MiniUpdateSpec) if spec.deps.nonEmpty =>
      val worker = startPackageUpload(services)(spec, spec.deps.head)
      context become running(services, spec, spec.deps.tail, worker, Set.empty)

    case Some(spec: MiniUpdateSpec) if spec.deps.isEmpty =>
      ()

    case None =>
      // do nothing in case no packages to upload. TODO why get here in that case? Should have quit earlier.
      ()

    case Status.Failure(t) =>
      log.error(t, "Unable to load update specifications.")
      context stop self
  }

  /**
   * Initial [[TransferProtocolActor]] behavior, waiting for the vehicle to [[StartDownload]].
   */
  override def receive : Receive = {
    case StartDownload(device, updateId, services) =>
      log.debug(s"$device requested update $updateId")
      import akka.pattern.pipe
      db.run(UpdateSpecs.load(device, updateId)) pipeTo self
      context become loadSpecs(services)
  }

}

/**
  * Sent by [[PackageTransferActor]] to the [[ConnectivityClient]] (step 1 of 3)
  * to indicate chunks are ready to be downloaded.
  */
case class StartDownloadMessage(update_id: UUID, checksum: String, chunkscount: Int)

object StartDownloadMessage {

  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[StartDownloadMessage] =
    deriveEncoder[StartDownloadMessage]

}

/**
  * The [[ConnectivityClient]] notifies which chunks it received for THE package being transferred.
  *
  * @param update_id of the [[UpdateRequest]]
  */
case class ChunksReceived(device: Device.Id, update_id: UUID, chunks: List[Int])

/**
  * Sent by [[PackageTransferActor]] to the [[ConnectivityClient]] (step 2 of 3) to transfer a chunk.
  * @param index is 1-based
  */
case class PackageChunk(update_id: UUID, bytes: ByteString, index: Int)

object PackageChunk {

  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[PackageChunk] =
    deriveEncoder[PackageChunk]

  implicit val byteStringEncoder : Encoder[ByteString] =
    Encoder[String].contramap[ByteString]( x => Base64.encodeBase64String(x.toArray) )

}

/**
  * Sent by [[PackageTransferActor]] to the [[ConnectivityClient]] (step 3 of 3) to indicate all chunks sent.
  *
  * @param signature Signature of the [[UpdateRequest]]. Not the [[Package.signature]].
  */
case class Finish(update_id: UUID, signature: String)

object Finish {

  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[Finish] =
    deriveEncoder[Finish]

}

case object UploadAborted

/**
 * Actor to handle transferring chunks of a single [[Package]] to a vehicle.
 *
 * @param updateId Unique Id of the update.
 * @param signature Signature of the [[UpdateRequest]]. Not the [[Package.signature]].
 * @param pckg the package to transfer
 * @param services the service paths available on the vehicle
 */
class PackageTransferActor(updateId: UUID,
                           signature: String,
                           pckg: Package,
                           services: ClientServices,
                           rviClient: ConnectivityClient,
                           s3PackageStoreOpt: Option[S3PackageStore])
    extends Actor with ActorLogging {

  import cats.syntax.show._
  import io.circe.generic.auto._
  import akka.pattern.pipe

  implicit val mat = ActorMaterializer()

  import context.system
  import context.dispatcher

  val chunkSize = system.settings.config.getBytes("rvi.transfer.chunkSize").intValue()
  val ackTimeout : FiniteDuration = FiniteDuration(
    system.settings.config.getDuration("rvi.transfer.ackTimeout", TimeUnit.MILLISECONDS),
    TimeUnit.MILLISECONDS
  )

  lazy val lastIndex = (BigDecimal(pckg.size) / BigDecimal(chunkSize) setScale(0, RoundingMode.CEILING)).toInt

  lazy val s3PackageStore = s3PackageStoreOpt.getOrElse(S3PackageStore(system.settings.config))
  val buffer = ByteBuffer.allocate( chunkSize )

  def ttl() : Instant = {
    Instant.now.plus(Duration.ofMinutes(5))
  }

  def sendChunk(channel: FileChannel, index: Int) : Unit = {
    log.debug( s"Sending chunk $index" )
    channel.position( (index - 1) * chunkSize )
    buffer.clear()
    val bytesRead = channel.read( buffer )
    buffer.flip()
    rviClient.sendMessage(services.chunk, PackageChunk(updateId, ByteString(buffer), index), ttl())
    context.setReceiveTimeout( ackTimeout )
  }

  def finish() : Unit = {
    rviClient.sendMessage(services.finish, Finish(updateId, signature), ttl())
    context stop self
  }

  override def preStart() : Unit = {
    log.debug(s"Starting transfer of the package $pckg. Chunk size: $chunkSize, chunks to transfer: $lastIndex")
    rviClient.sendMessage( services.start, StartDownloadMessage(updateId, pckg.checkSum, lastIndex), ttl() )
  }

  val maxAttempts : Int = 5

  private def openChannel(uri: URI): FileChannel = {
    FileChannel.open(Paths.get(uri), StandardOpenOption.READ)
  }

  private def isLocalFile(uri: Uri): Boolean = {
    Try(new File(new URI(uri.toString())).exists()).getOrElse(false)
  }

  /**
    * Makes sure the package URI is local, so it can be read by this actor
    * and transferred to the client
   */
  def downloadingRemotePackage(): Receive = {
    if(isLocalFile(pckg.uri)) {
      self ! (new URI(pckg.uri.toString()))
    } else {
      s3PackageStore
        .retrieveFile(packageUri = pckg.uri)
        .map(_.toURI).pipeTo(self)
    }

    {
      case uri: URI =>
        val channel = openChannel(uri)
        sendChunk(channel, 1)
        context.become(transferring(channel, 1, 1))

      case Status.Failure(ex) =>
        log.error(ex, "Could not download remote file")
        context.parent ! UploadAborted
        context.stop(self)

      case msg =>
        log.warning("Unexpected msg received {}", msg)
    }
  }

  // scalastyle:off
  /**
   * Send the next chunk or resend last chunk if vehicle doesn't acknowledge with [[ChunksReceived]].
   * Abort transfer if maxAttempts exceeded.
   */
  def transferring(channel: FileChannel, lastSentChunk: Int, attempt: Int) : Receive = {
    case ChunksReceived(_, _, indexes) =>
      log.debug(s"${pckg.id.show}. Chunk received by client: $indexes" )
      val mayBeNext = indexes.sorted.sliding(2, 1).find {
        case first :: second :: Nil => second - first > 1
        case first :: Nil => true
        case _ => false
      }.map {
        case first :: _ :: Nil => first + 1
        case 1 :: Nil => 2
        case Nil => 1
      }
      val nextIndex = mayBeNext.getOrElse(indexes.max + 1)
      log.debug(s"Next chunk index: $nextIndex")
      if( nextIndex > lastIndex ) finish()
      else {
        sendChunk(channel, nextIndex)
        context.become( transferring(channel, nextIndex, 1) )
      }

    case ReceiveTimeout if attempt == maxAttempts =>
      context.setReceiveTimeout(scala.concurrent.duration.Duration.Undefined)
      context.parent ! UploadAborted
      context.stop( self )

    case ReceiveTimeout =>
      sendChunk(channel, lastSentChunk)
      context.become( transferring(channel, lastSentChunk, attempt + 1) )
  }
  // scalastyle:on

  /**
   * Entry point to this actor starting with first chunk.
   */
  override def receive: Receive = {
    case ChunksReceived(_, _, Nil) =>
      context.become(downloadingRemotePackage())

  }

  override def postStop() : Unit = {
  }

}

object PackageTransferActor {

  /**
   * Configuration for creating [[PackageTransferActor]].
   *
   * @param signature Signature of the [[UpdateRequest]]. Not the [[Package.signature]].
   */
  def props(rviClient: ConnectivityClient,
            s3PackageStoreOpt: Option[S3PackageStore] = None)
           (updateId: UUID, signature: String, pckg: Package, services: ClientServices): Props =
    Props(new PackageTransferActor(updateId, signature, pckg, services, rviClient, s3PackageStoreOpt))

}
