/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.daemon

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.syntax.show._
import com.advancedtelematic.libats.data.Namespace
import com.advancedtelematic.libats.data.RefinedUtils.RefineTry
import com.advancedtelematic.libtuf.data.TufDataType.{Checksum, HashMethod, ValidChecksum}
import com.advancedtelematic.libtuf.reposerver.ReposerverClient
import java.util.UUID
import org.genivi.sota.core.Settings
import org.genivi.sota.core.UpdateService
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.storage.StoragePipeline
import org.genivi.sota.data.PackageId
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages._
import org.slf4j.LoggerFactory
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try
import slick.jdbc.MySQLProfile.api.Database

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

  lazy val storagePipeline = new StoragePipeline(updateService)

  def action(event: TreehubCommit)
            (implicit ec: ExecutionContext): Future[Done] = for {
    pid  <- Future.fromTry(mkPkgId(event))
    pkg   = mkPkg(event, pid)
    _    <- storagePipeline.storePackage(pkg)
    _    <- publishToTuf(event, pid)
  } yield Done

  /***************************************************************************/

  def mkPkgId(event: TreehubCommit): Try[PackageId] = for {
    n <- (event.refName).refineTry[PackageId.ValidName]
    v <- (event.commit).refineTry[PackageId.ValidVersion]
  } yield PackageId(n, v)

  def mkPkg(event: TreehubCommit, pid: PackageId): Package =
    Package(event.ns, UUID.randomUUID(), pid, event.uri, event.size, event.commit, Some(event.description), None, None)

  def publishToTuf(event: TreehubCommit, pid: PackageId): Future[Unit] = for {
    hash <- Future.fromTry(event.commit.refineTry[ValidChecksum])
    _    <- tufClient.addTarget(Namespace(event.ns.get), pid.show, event.uri,
                                Checksum(HashMethod.SHA256, hash), event.size)
  } yield ()

}
