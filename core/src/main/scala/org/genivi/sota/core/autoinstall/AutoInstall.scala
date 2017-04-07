/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core.autoinstall

import akka.Done
import akka.http.scaladsl.util.FastFuture
import org.genivi.sota.core.campaigns.CampaignLauncher
import org.genivi.sota.core.db.AutoInstalls
import org.genivi.sota.core.UpdateService
import org.genivi.sota.data.{Namespace, PackageId, Uuid}
import org.genivi.sota.messaging.MessageBusPublisher
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}

object AutoInstall {
  import UpdateService.DependencyResolver

  def resolve(devices: Seq[Uuid]): DependencyResolver = { pkg =>
    Future.successful(devices.map(_ -> Set(pkg.id)).toMap)
  }

  def packageCreated(ns: Namespace, pkgId: PackageId, updateService: UpdateService, messageBus: MessageBusPublisher)
                    (implicit db: Database, ec: ExecutionContext): Future[Done] = {
    db.run(AutoInstalls.listDevices(ns, pkgId.name)).flatMap {
      case Nil => FastFuture.successful(Done)
      case devices => for {
        updateRequest <- updateService.updateRequest(ns, pkgId)
        _ <- updateService.queueUpdate(ns, updateRequest, resolve(devices), messageBus)
        _ <- CampaignLauncher.sendMsg(ns, devices.toSet, pkgId, Uuid.fromJava(updateRequest.id), messageBus)
      } yield Done
    }
  }
}
