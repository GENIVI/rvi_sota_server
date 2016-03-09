/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.rvi

import akka.actor.ReceiveTimeout
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Paths, StandardOpenOption}

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.util.ByteString
import io.circe.Encoder
import java.util.concurrent.TimeUnit
import org.apache.commons.codec.binary.Base64
import org.genivi.sota.core.data.UpdateStatus
import org.genivi.sota.core.data.Vehicle
import org.genivi.sota.core.data.{Package, UpdateSpec, Vehicle}
import org.genivi.sota.core.db.{UpdateSpecs, InstallHistories}
import org.joda.time.DateTime
import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.util.control.NoStackTrace
import slick.driver.MySQLDriver.api.Database

import scala.collection.immutable.Queue
import scala.math.BigDecimal.RoundingMode

/**
 * Actor to handle events received from the RVI node.
 *
 * @param transferActorProps the configuration class for creating actors to handle a single vehicle
 * @see SotaServices
 */
class UpdateController(transferProtocolProps: Props) extends Actor with ActorLogging {

  /**
   * Create actors to handle new downloads to a single vehicle.
   * Forward messages to that actor until it finishes the download.
   * Remove the actor from the map when it is terminated.
   *
   * @param currentDownloads the map of VIN to the actor handling that vehicle
   */
  def running( currentDownloads: Map[Vehicle.Vin, ActorRef] ) : Receive = {
    case x @ StartDownload(vin, packages, clientServices) =>
      currentDownloads.get(vin) match {
        case None =>
          log.debug(s"New transfer to vehicle $vin for $packages, count ${currentDownloads.size}")
          val actor = context.actorOf( transferProtocolProps )
          context.watch(actor)
          context.become( running( currentDownloads.updated( vin, actor ) ) )
          actor ! x
        case Some(x) =>
          log.warning(
            s"There is an active transfer for vehicle $vin. Request to transfer packages $packages will be ignored." )
      }
    case x @ ChunksReceived(vin, _, _) =>
      currentDownloads.get(vin).foreach( _ ! x)

    case x: InstallReport =>
      log.debug(s"Install report from vehicle ${x.vin}")
      currentDownloads.get(x.vin).foreach( _ ! x )

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
   * Configuration class for creating the UpdateController actor.
   */
  def props( transferProtocolProps: Props ) : Props = Props( new UpdateController(transferProtocolProps) )

}

object TransferProtocolActor {

  private[TransferProtocolActor] case class Specs( values : Iterable[UpdateSpec])

  /**
   * Configuration class for creating the TransferProtocolActor actor.
   */
  def props(db: Database, rviClient: RviClient, transferActorProps: (ClientServices, Package) => Props) =
    Props( new TransferProtocolActor( db, rviClient, transferActorProps) )
}

object UpdateEvents {

  /**
   * Message from TransferProtocolActor when all packages are transferred to vehicle.
   */
  final case class PackagesTransferred( update: UpdateSpec )

  /**
   * Message from TransferProtocolActor when an installation report is received from the vehicle.
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
class TransferProtocolActor(db: Database, rviClient: RviClient, transferActorProps: (ClientServices, Package) => Props)
    extends Actor with ActorLogging {
  import context.dispatcher
  import cats.syntax.show._
  import cats.syntax.eq._

  val installTimeout : FiniteDuration = FiniteDuration(
    context.system.settings.config.getDuration("rvi.transfer.installTimeout", TimeUnit.MILLISECONDS),
    TimeUnit.MILLISECONDS
  )

  def buildTransferQueue(specs: Iterable[UpdateSpec]) : Queue[Package] = {
    specs.foldLeft(Set.empty[Package])( (acc, x) => acc.union( x.dependencies ) ).to[Queue]
  }

  /**
   * Create actors to handle each transfer to the vehicle.
   * Forward ChunksReceived messages from the vehicle to the transfer actors.
   * Terminate when all updates and dependencies are successfully transferred,
   * or when the transfer is aborted because the vehicle is not responding.
   */
  def running(services: ClientServices, updates: Set[UpdateSpec], pending: Queue[Package],
              inProgress: Map[ActorRef, Package], done: Set[Package]) : Receive = {
    case akka.actor.Terminated(ref) =>
      // TODO: Handle actors terminated on errors (e.g. file exception in PackageTransferActor)
      val p = inProgress.get(ref).get
      log.debug(s"Package $p uploaded.")
      val newInProgress = pending.headOption.map( startPackageUpload(services) )
        .fold( inProgress - ref)( inProgress - ref + _ )
      if( newInProgress.isEmpty ) {
        log.debug( s"All packages uploaded." )
        context.setReceiveTimeout(installTimeout)
        updates.foreach { x =>
          context.system.eventStream.publish( UpdateEvents.PackagesTransferred( x ) )
        }
      } else {
        val finishedPackageTransfers = done + p
        val (finishedUpdates, unfinishedUpdates) = updates.span(_.dependencies.diff( finishedPackageTransfers ).isEmpty)
        context.become(
          running( services, unfinishedUpdates, if(pending.isEmpty) pending else pending.tail,
                   newInProgress, finishedPackageTransfers) )
      }

    case x @ ChunksReceived(vin, p, _) =>
      inProgress.find( _._2.id == p).foreach( _._1 ! x)

    case r @ InstallReport(vin, packageId, success, msg) =>
      context.system.eventStream.publish( UpdateEvents.InstallReportReceived(r) )
      log.debug(s"Install report received from $vin: ${packageId.show} installed $success, $msg")
      updates.find( _.request.packageId === packageId ).foreach { finishedSpec =>
        val pendingSpecs = updates - finishedSpec
        db.run( UpdateSpecs.setStatus( finishedSpec, if( success ) UpdateStatus.Finished  else UpdateStatus.Failed ) )
        db.run( InstallHistories.log(vin, packageId, success) )
        if( pendingSpecs.isEmpty ) {
          log.debug( "All installation reports received." )
          rviClient.sendMessage(services.getpackages, io.circe.Json.Empty, ttl())
          context.stop( self )
        } else {
          context.become(running( services, pendingSpecs, pending, inProgress, done ))
        }
      }

    case ReceiveTimeout =>
      abortUpdate(services, updates)

    case UploadAborted =>
      abortUpdate(services, updates)
  }

  def abortUpdate (services: ClientServices, updates: Set[UpdateSpec]) = {
      rviClient.sendMessage(services.abort, io.circe.Json.Empty, ttl())
      updates.foreach(x => db.run( UpdateSpecs.setStatus(x, UpdateStatus.Canceled) ))
      context.stop(self)
  }

  def ttl() : DateTime = {
    import com.github.nscala_time.time.Implicits._
    DateTime.now + 5.minutes
  }

  def startPackageUpload( services: ClientServices)( p: Package ) : (ActorRef, Package) = {
    val ref = context.actorOf( transferActorProps(services, p), s"${p.id.name.get}-${p.id.version.get}")
    context.watch(ref)
    ref -> p
  }

  def loadSpecs(services: ClientServices) : Receive = {
    case TransferProtocolActor.Specs(values) =>
      val todo = buildTransferQueue(values)
      val workers : Map[ActorRef, Package] = todo.take(3).map( startPackageUpload(services) ).toMap
      context become running( services, values.toSet, todo.drop(3), workers, Set.empty )

    case Status.Failure(t) =>
      log.error(t, "Unable to load update specifications.")
      context stop self
  }

  /**
   * Entry point to this actor when the vehicle initiates a download.
   */
  override def receive : Receive = {
    case StartDownload(vin, packages, services) =>
      log.debug(s"$vin requested packages $packages")
      import akka.pattern.pipe
      db.run( UpdateSpecs.load(vin, packages.toSet) ).map(TransferProtocolActor.Specs.apply) pipeTo self
      context become loadSpecs(services)
  }

}

case class StartDownloadMessage( `package`: Package.Id, checksum: String, chunkscount: Int )

object StartDownloadMessage {

  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[StartDownloadMessage] =
    deriveFor[StartDownloadMessage].encoder

}

case class ChunksReceived( vin: Vehicle.Vin, `package`: Package.Id, chunks: List[Int] )

case class PackageChunk( `package`: Package.Id, bytes: ByteString, index: Int)

object PackageChunk {

  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[PackageChunk] =
    deriveFor[PackageChunk].encoder

  implicit val byteStringEncoder : Encoder[ByteString] =
    Encoder[String].contramap[ByteString]( x => Base64.encodeBase64String(x.toArray) )

}

case class Finish(`package`: Package.Id )

object Finish {

  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[Finish] =
    deriveFor[Finish].encoder

}

case object UploadAborted

/**
 * Actor to handle transferring chunks to a vehicle.
 *
 * @param pckg the package to transfer
 * @param services the service paths available on the vehicle
 */
class PackageTransferActor( pckg: Package, services: ClientServices, rviClient: RviClient )
    extends Actor with ActorLogging {

  import io.circe.generic.auto._
  import context.dispatcher
  import cats.syntax.show._

  val chunkSize = context.system.settings.config.getBytes("rvi.transfer.chunkSize").intValue()
  val ackTimeout : FiniteDuration = FiniteDuration(
    context.system.settings.config.getDuration("rvi.transfer.ackTimeout", TimeUnit.MILLISECONDS),
    TimeUnit.MILLISECONDS
  )


  lazy val lastIndex = (BigDecimal(pckg.size) / BigDecimal(chunkSize) setScale(0, RoundingMode.CEILING)).toInt

  val channel = FileChannel.open( Paths.get( new URI( pckg.uri.toString() ) ), StandardOpenOption.READ)
  val buffer = ByteBuffer.allocate( chunkSize )

  def ttl() : DateTime = {
    import com.github.nscala_time.time.Implicits._
    DateTime.now + 5.minutes
  }

  def sendChunk(index: Int) : Unit = {
    log.debug( s"Sending chunk $index" )
    channel.position( (index - 1) * chunkSize )
    buffer.clear()
    val bytesRead = channel.read( buffer )
    buffer.flip()
    rviClient.sendMessage( services.chunk, PackageChunk( pckg.id,  ByteString( buffer ), index), ttl() )
    context.setReceiveTimeout( ackTimeout )
  }

  def finish() : Unit = {
    rviClient.sendMessage( services.finish, Finish(pckg.id), ttl() )
    context stop self
  }

  override def preStart() : Unit = {
    log.debug(s"Starting transfer of the package $pckg. Chunk size: $chunkSize, chunks to transfer: $lastIndex")
    rviClient.sendMessage( services.start, StartDownloadMessage(pckg.id, pckg.checkSum, lastIndex), ttl() )
  }

  val maxAttempts : Int = 5

  /**
   * Send the next chunk or resend last chunk if vehicle doesn't acknowledge with ChunksReceived.
   * Abort transfer if maxAttempts exceeded.
   */
  def transferring( lastSentChunk: Int, attempt: Int ) : Receive = {
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
        sendChunk(nextIndex)
        context.become( transferring(nextIndex, 1) )
      }

    case ReceiveTimeout if attempt == maxAttempts =>
      context.setReceiveTimeout(Duration.Undefined)
      context.parent ! UploadAborted
      context.stop( self )

    case ReceiveTimeout =>
      sendChunk(lastSentChunk)
      context.become( transferring(lastSentChunk, attempt + 1) )
  }

  /**
   * Entry point to this actor starting with first chunk.
   */
  override def receive = {
    case ChunksReceived(_, _, Nil) =>
      sendChunk(1)
      context.become( transferring(1, 1) )

  }

  override def postStop() : Unit = {
    channel.close()
  }

}

object PackageTransferActor {

  /**
   * Configuration class for creating PackageTransferActor.
   */
  def props( rviClient: RviClient )( services: ClientServices, pckg: Package) : Props =
    Props( new PackageTransferActor(pckg, services, rviClient) )

}
