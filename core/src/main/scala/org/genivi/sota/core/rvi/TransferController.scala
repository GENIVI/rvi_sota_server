/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.rvi

import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.{Paths, StandardOpenOption}

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status}
import akka.util.ByteString
import io.circe.Encoder
import org.apache.commons.codec.binary.Base64
import org.genivi.sota.core.data.UpdateStatus
import org.genivi.sota.core.data.Vehicle
import org.genivi.sota.core.data.{Package, UpdateSpec, Vehicle}
import org.genivi.sota.core.db.UpdateSpecs
import org.joda.time.DateTime
import slick.driver.MySQLDriver.api.Database

import scala.collection.immutable.Queue
import scala.math.BigDecimal.RoundingMode

class UpdateController(transferProtocolProps: Props) extends Actor with ActorLogging {

  def running( currentDownloads: Map[Vehicle.IdentificationNumber, ActorRef] ) : Receive = {
    case x @ StartDownload(vin, packages, clientServices) =>
      currentDownloads.get(vin) match {
        case None =>
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
      currentDownloads.get(x.vin).foreach( _ ! x )

    case akka.actor.Terminated(x) =>
      context.become( running( currentDownloads.filterNot( _._2 == x ) ) )
  }

  override def receive : Receive = running( Map.empty )

}

object UpdateController {

  def props( transferProtocolProps: Props ) : Props = Props( new UpdateController(transferProtocolProps) )

}

object TransferProtocolActor {

  private[TransferProtocolActor] case class Specs( values : Iterable[UpdateSpec])

  def props(db: Database, rviClient: RviClient, transferActorProps: (ClientServices, Package) => Props) =
    Props( new TransferProtocolActor( db, rviClient, transferActorProps) )

  case object GetAllPackages

}

object UpdateEvents {

  final case class PackagesTransferred( update: UpdateSpec )

  final case class InstallReportReceived( report: InstallReport )

}

class TransferProtocolActor(db: Database, rviClient: RviClient, transferActorProps: (ClientServices, Package) => Props)
    extends Actor with ActorLogging {
  import context.dispatcher
  import cats.syntax.show._
  import cats.syntax.eq._

  def buildTransferQueue(specs: Iterable[UpdateSpec]) : Queue[Package] = {
    specs.foldLeft(Set.empty[Package])( (acc, x) => acc.union( x.dependencies ) ).to[Queue]
  }

  def running(services: ClientServices, updates: Set[UpdateSpec], pending: Queue[Package],
              inProgress: Map[ActorRef, Package], done: Set[Package]) : Receive = {
    case akka.actor.Terminated(ref) =>
      val p = inProgress.get(ref).get
      log.debug(s"Package $p uploaded.")
      val newInProgress = pending.headOption.map( startPackageUpload(services) )
        .fold( inProgress - ref)( inProgress - ref + _ )
      if( newInProgress.isEmpty ) {
        log.debug( s"All packages uploaded." )
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
        if( pendingSpecs.isEmpty ) {
          log.debug( "All installation reports received." )
          rviClient.sendMessage(services.getpackages, io.circe.Json.Empty, ttl())
          context.stop( self )
        } else {
          context.become(running( services, pendingSpecs, pending, inProgress, done ))
        }
      }

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

  override def receive : Receive = {
    case StartDownload(vin, packages, services) =>
      log.debug(s"$vin requested packages $packages")
      import akka.pattern.pipe
      db.run( UpdateSpecs.load(vin, packages.toSet) ).map(TransferProtocolActor.Specs.apply) pipeTo self
      context become loadSpecs(services)
  }

}

case class StartDownloadMessage( `package`: Package.Id, checksum: String, chunkscount: Int )
case class ChunksReceived( vin: Vehicle.IdentificationNumber, `package`: Package.Id, chunks: List[Int] )
case class PackageChunk( `package`: Package.Id, bytes: ByteString, index: Int)
case class Finish(`package`: Package.Id )

class PackageTransferActor( pckg: Package, services: ClientServices, rviClient: RviClient )
    extends Actor with ActorLogging {

  implicit val byteStringEncoder : Encoder[ByteString] =
    Encoder[String].contramap[ByteString]( x => Base64.encodeBase64String(x.toArray) )

  import io.circe.generic.auto._
  import context.dispatcher
  import cats.syntax.show._

  val chunkSize = context.system.settings.config.getBytes("rvi.transfer.chunkSize").intValue()

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
  }

  def finish() : Unit = {
    rviClient.sendMessage( services.finish, Finish(pckg.id), ttl() )
    context stop self
  }

  override def preStart() : Unit = {
    log.debug(s"Starting transfer of the package $pckg. Chunk size: $chunkSize, chunks to transfer: $lastIndex")
    rviClient.sendMessage( services.start, StartDownloadMessage(pckg.id, pckg.checkSum, lastIndex), ttl() )
  }

  override def receive = {
    case ChunksReceived(_, _, Nil) =>
      sendChunk(1)

    case ChunksReceived(_, _, indexes) =>
      log.debug(s"${pckg.id.show}. Chunk received by client: $indexes" )
      val mayBeNext = indexes.sorted.sliding(2, 1).find {
        case first :: second :: Nil => second - first > 1
        case first :: Nil => true
      }.map {
        case first :: _ :: Nil => first + 1
        case 1 :: Nil => 2
      }
      val nextIndex = mayBeNext.getOrElse(indexes.max + 1)
      log.debug(s"Next chunk index: $nextIndex")
      if( nextIndex > lastIndex ) finish() else sendChunk(nextIndex)
  }

  override def postStop() : Unit = {
    channel.close()
  }

}

object PackageTransferActor {

  def props( rviClient: RviClient )( services: ClientServices, pckg: Package) : Props =
    Props( new PackageTransferActor(pckg, services, rviClient) )

}
