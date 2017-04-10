package org.genivi.sota.core.daemon

import akka.Done
import cats.data.Xor
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.UpdateService
import org.genivi.sota.core.campaigns.CampaignLauncher
import org.genivi.sota.core.db.{Campaigns, Packages}
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.{DeltaGenerationFailed, GeneratedDelta}
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.data.Campaign
import org.slf4j.LoggerFactory
import org.genivi.sota.messaging.Commit.Commit

import scala.concurrent.{ExecutionContext, Future}

class DeltaListener(deviceRegistry: DeviceRegistry, updateService: UpdateService,
                    messageBus: MessageBusPublisher)(implicit db: Database)  {

  private val log = LoggerFactory.getLogger(this.getClass)

  /**
    * this method only returns a DBIO so it can be used inside a for comprehension involving slick calls
    */
  private def validateMessage(campaign: Campaign, from: Commit, to: Commit)
                             (implicit ec: ExecutionContext): DBIO[Done] = {
    import org.genivi.sota.data.Uuid
    val meta = campaign.meta
    val preCheck: Xor[String, Uuid] = for {
      deltaFrom <- Xor.fromEither(meta.deltaFrom.toRight("Received GeneratedDelta message for campaign without static" +
                                                         " delta"))
      _ <- if (deltaFrom.version.get.equalsIgnoreCase(from.get)) Xor.Right(Unit)
      else Xor.Left("Received GeneratedDelta message for campaign with differing from version")
      pkg <- Xor.fromEither(meta.packageUuid.toRight("Received GeneratedDelta message for campaign without a " +
                                                     "target version"))
    } yield pkg

    preCheck match {
      case Xor.Left(err) => DBIO.failed(new IllegalArgumentException(err))
      case Xor.Right(_) => Packages.byUuid(campaign.meta.packageUuid.get.toJava).flatMap { pkg =>
        if (pkg.id.version.get.equalsIgnoreCase(to.get)) {
          DBIO.successful(Done)
        } else {
          DBIO.failed(new IllegalArgumentException(s"Version in GeneratedDelta message ($to) doesn't match version " +
            s"in campaign (${pkg.id.version})"))
        }
      }
    }
  }

  def generatedDeltaAction(msg: GeneratedDelta)(implicit ec: ExecutionContext): Future[Done] = {
    val id = Campaign.Id(msg.id)
    val f = for {
      campaign <- Campaigns.fetch(id)
      _        <- validateMessage(campaign, msg.from, msg.to)
      _        <- Campaigns.setSize(id, msg.size)
      lc       <- Campaigns.fetchLaunchCampaignRequest(id)
    } yield lc

    db.run(f.transactionally).flatMap { lc =>
      CampaignLauncher.launch(deviceRegistry, updateService, id, lc, messageBus)(db, ec).map(_ => Done)
    }
  }

  def deltaGenerationFailedAction(msg: DeltaGenerationFailed)(implicit ec: ExecutionContext): Future[Done] = {
    log.error(s"Delta generation for campaign ${msg.id} failed with error: ${msg.error.getOrElse("")}")
    db.run(Campaigns.setAsDraft(Campaign.Id(msg.id))).map(_ => Done)
  }
}
