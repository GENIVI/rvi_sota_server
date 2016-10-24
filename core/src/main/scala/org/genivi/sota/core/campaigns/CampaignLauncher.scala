/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core.campaigns

import akka.actor.ActorSystem
import cats.implicits._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.UpdateService
import org.genivi.sota.core.data.{Campaign, UpdateRequest}
import org.genivi.sota.core.db.Campaigns
import org.genivi.sota.data.{Interval, Namespace, PackageId, Uuid}
import slick.driver.MySQLDriver.api.Database

import scala.concurrent.{ExecutionContext, Future}

object CampaignLauncher {
  import Campaign._
  import UpdateService._

  def resolve(devices: Seq[Uuid]): DependencyResolver = { pkg =>
    Future.successful(devices.map(_ -> Set(pkg.id)).toMap)
  }

  def launch (deviceRegistry: DeviceRegistry, updateService: UpdateService, id: Campaign.Id, lc: LaunchCampaign)
             (implicit db: Database, system: ActorSystem, ec: ExecutionContext): Future[List[Uuid]] = {
    def updateUpdateRequest(ur: UpdateRequest): UpdateRequest = {
      ur.copy(periodOfValidity = Interval(lc.startDate.getOrElse(ur.periodOfValidity.start),
                                          lc.endDate.getOrElse(ur.periodOfValidity.end)),
              priority = lc.priority.getOrElse(ur.priority),
              signature = lc.signature.getOrElse(ur.signature),
              description = lc.description,
              requestConfirmation = lc.requestConfirmation.getOrElse(ur.requestConfirmation)
      )
    }

    def launchGroup (ns: Namespace, pkgId: PackageId, campGrp: CampaignGroup): Future[Uuid] = {
      val groupId = campGrp.group
      for {
        updateRequest <- updateService.updateRequest(ns, pkgId).map(updateUpdateRequest)
        devices       <- deviceRegistry.fetchGroup(groupId)
        _             <- updateService.queueUpdate(ns, updateRequest, resolve(devices))

        uuid          = Uuid.fromJava(updateRequest.id)
        _             <- db.run(Campaigns.setUpdateUuid(id, groupId, uuid))
      } yield uuid
    }

    for {
      camp       <- db.run(Campaigns.setAsLaunch(id))
      updateRefs <- camp.groups.toList.traverse(campGrp =>
        launchGroup(camp.meta.namespace, camp.packageId.get, campGrp))
    } yield updateRefs
  }
}
