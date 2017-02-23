/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.daemon

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.syntax.show._
import com.advancedtelematic.libats.data.Namespace
import com.advancedtelematic.libats.data.RefinedUtils.RefineTry
import com.advancedtelematic.libtuf.data.TufDataType.{Checksum, HashMethod, ValidChecksum}
import com.advancedtelematic.libtuf.reposerver.ReposerverClient
import java.util.UUID
import org.genivi.sota.core.DigestCalculator.DigestResult
import org.genivi.sota.core.Settings
import org.genivi.sota.core.UpdateService
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.storage.PackageStorage
import org.genivi.sota.core.storage.PackageStorage.{PackageSize, PackageStorageOp}
import org.genivi.sota.core.storage.StoragePipeline
import org.genivi.sota.data.PackageId
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages._
import org.slf4j.LoggerFactory
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try
import slick.driver.MySQLDriver.api.Database

class TreehubCommitListener(db: Database,
                            updateService: UpdateService,
                            tufClient: ReposerverClient,
                            bus: MessageBusPublisher)
                           (implicit system: ActorSystem,
                            mat: ActorMaterializer,
                            ec: ExecutionContext) extends Settings {

  case class ImageRequest(commit: String,
                          refName: String,
                          description: String,
                          pullUri: String)

  implicit val _config = system.settings.config
  implicit val _db = db
  implicit val _bus = bus
  val log = LoggerFactory.getLogger(this.getClass)

  val packageStorageOp: PackageStorageOp = new PackageStorage().store _
  lazy val storagePipeline = new StoragePipeline(updateService, packageStorageOp)

  def action(event: TreehubCommit)
            (implicit ec: ExecutionContext): Future[Done] = for {
    pid  <- Future.fromTry(mkPkgId(event))
    _    <- storagePipeline.storePackage(event.ns, pid, mkPayload(event, pid), mkPkg(event, pid))
    _    <- publishToTuf(event, pid)
  } yield Done

  /***************************************************************************/

  def mkPkgId(event: TreehubCommit): Try[PackageId] = for {
    n <- (s"treehub-${event.refName}").refineTry[PackageId.ValidName]
    v <- (event.commit).refineTry[PackageId.ValidVersion]
  } yield PackageId(n, v)

  def mkPkg(event: TreehubCommit, pid: PackageId)
           (uri: Uri, _size: PackageSize, digest: DigestResult): Package =
      Package(event.ns, UUID.randomUUID(), pid, uri, event.size, digest, Some(event.description), None, None)

  def mkPayload(event: TreehubCommit, pid: PackageId): Source[ByteString, Any] = {
    import io.circe.generic.auto._
    import io.circe.syntax._

    val payload = ImageRequest(event.commit, event.refName, event.description, event.uri).asJson.noSpaces

    Source(Vector(ByteString(payload)))
  }

  def publishToTuf(event: TreehubCommit, pid: PackageId): Future[Unit] = for {
    hash <- Future.fromTry(event.commit.refineTry[ValidChecksum])
    _    <- tufClient.addTarget(Namespace(event.ns.get), pid.show, event.uri,
                                Checksum(HashMethod.SHA256, hash), event.size)
  } yield ()

}
