/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core.campaigns

import org.genivi.sota.core.UpdateService
import org.genivi.sota.core.data.Campaign.CampaignGroup
import org.genivi.sota.core.data.{Campaign, CampaignStatistics}
import org.genivi.sota.core.db.Campaigns
import org.genivi.sota.data.{UpdateStatus, Uuid}
import slick.jdbc.MySQLProfile.api.Database

import scala.concurrent.{ExecutionContext, Future}

object CampaignStats {

  private def getCampaignStatsForUpdate(groupId: Uuid, updateId: Uuid, updateService: UpdateService)
                                       (implicit db: Database, ec: ExecutionContext)
  : Future[CampaignStatistics] = {
    for {
      rows <- updateService.fetchUpdateSpecRows(updateId)
      successCount  = rows.count(r => r.status.equals(UpdateStatus.Finished))
      failureCount  = rows.count(r => r.status.equals(UpdateStatus.Failed))
      cancelledCount = rows.count(r => r.status.equals(UpdateStatus.Canceled))
      updatedCount  = successCount + failureCount + cancelledCount
    } yield CampaignStatistics(groupId, updateId, rows.size, updatedCount, successCount, failureCount, cancelledCount)
  }


  def get(id: Campaign.Id, updateService: UpdateService)
         (implicit db: Database, ec: ExecutionContext)
  : Future[Seq[CampaignStatistics]] = {
    db.run(Campaigns.fetchGroups(id)).flatMap { groups =>
      Future.sequence(
        groups.collect {
          case CampaignGroup(groupId, Some(updateId)) =>
            getCampaignStatsForUpdate(groupId, updateId, updateService)
        })
    }
  }
}
