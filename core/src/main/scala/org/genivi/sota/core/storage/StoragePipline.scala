/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.storage

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import java.time.Instant
import org.genivi.sota.core.DigestCalculator.DigestResult
import org.genivi.sota.core.UpdateService
import org.genivi.sota.core.autoinstall.AutoInstall
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.Packages
import org.genivi.sota.core.storage.PackageStorage.{PackageSize, PackageStorageOp}
import org.genivi.sota.data.Namespace
import org.genivi.sota.data.PackageId
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.MySQLDriver.api.Database

class StoragePipeline(updateService: UpdateService, packageStorageOp: PackageStorageOp)
                     (implicit ec: ExecutionContext, db: Database,
                      system: ActorSystem, mat: ActorMaterializer,
                      bus: MessageBusPublisher) {

  implicit val _config = system.settings.config

  def storePackage(ns: Namespace,
                   pid: PackageId,
                   file: Source[ByteString, Any],
                   mkPkgFn: (Uri, PackageSize, DigestResult) => Package)
                   : Future[Unit] = {

    for {
      (uri, size, digest) <- packageStorageOp(pid, ns.get, file)
      pkg    = mkPkgFn(uri, size, digest)
      usage <- db.run(Packages.create(pkg).andThen(Packages.usage(pkg.namespace)))
      _     <- AutoInstall.packageCreated(pkg.namespace, pkg.id, updateService)
      _     <- bus.publishSafe(PackageCreated(pkg.namespace, pkg.id, pkg.description, pkg.vendor, pkg.signature))
      _     <- bus.publishSafe(PackageStorageUsage(ns, Instant.now, usage))
    } yield ()
  }

}
