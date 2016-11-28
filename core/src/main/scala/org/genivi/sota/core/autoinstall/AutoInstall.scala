/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core.autoinstall

import org.genivi.sota.core.db.AutoInstalls
import org.genivi.sota.core.UpdateService
import org.genivi.sota.data.Uuid
import org.genivi.sota.messaging.Messages.PackageCreated

import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}

object AutoInstall {
  import UpdateService.DependencyResolver

  def resolve(devices: Seq[Uuid]): DependencyResolver = { pkg =>
    Future.successful(devices.map(_ -> Set(pkg.id)).toMap)
  }

  def packageCreated(updateService: UpdateService, msg: PackageCreated)
                    (implicit db: Database, ec: ExecutionContext): Future[Unit] = {
    val ns = msg.namespace
    val pkgId = msg.packageId

    for {
      updateRequest <- updateService.updateRequest(ns, pkgId)
      devices <- db.run(AutoInstalls.listDevices(ns, pkgId.name))
      _ <- updateService.queueUpdate(ns, updateRequest, resolve(devices))
    } yield ()
  }
}
