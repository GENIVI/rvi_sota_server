/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.storage

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import java.time.Instant
import org.genivi.sota.core.UpdateService
import org.genivi.sota.core.autoinstall.AutoInstall
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.Packages
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages._
import scala.concurrent.{Future, ExecutionContext}
import slick.jdbc.MySQLProfile.api.Database

class StoragePipeline(updateService: UpdateService)
                     (implicit ec: ExecutionContext, db: Database,
                      system: ActorSystem, mat: ActorMaterializer,
                      bus: MessageBusPublisher) {

  implicit val _config = system.settings.config

  def storePackage(pkg: Package): Future[Unit] = {

    for {
      usage <- db.run(Packages.create(pkg).andThen(Packages.usage(pkg.namespace)))
      _     <- AutoInstall.packageCreated(pkg.namespace, pkg.id, updateService, bus)
      _     <- bus.publishSafe(PackageCreated(pkg.namespace, pkg.id, pkg.description, pkg.vendor, pkg.signature,
                                              Instant.now()))
      _     <- bus.publishSafe(PackageStorageUsage(pkg.namespace, Instant.now, usage))
    } yield ()
  }

}
