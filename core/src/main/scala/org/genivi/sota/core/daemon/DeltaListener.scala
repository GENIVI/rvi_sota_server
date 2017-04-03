package org.genivi.sota.core.daemon

import akka.Done
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.UpdateService
import org.genivi.sota.core.campaigns.CampaignLauncher
import org.genivi.sota.core.db.{Campaigns, Packages}
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.{GeneratedDelta, GeneratingDeltaFailed}
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.data.Campaign
import org.genivi.sota.data.PackageId
import org.slf4j.LoggerFactory
import eu.timepit.refined._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.async.Async._

class DeltaListener(deviceRegistry: DeviceRegistry, updateService: UpdateService,
                    messageBus: MessageBusPublisher)(implicit db: Database)  {

  private val log = LoggerFactory.getLogger(this.getClass)

  private def parseVersion(version: String): Option[PackageId.Version] =
    refineV[PackageId.ValidVersion](version) match {
      case Left(_) => log.error(s"Received invalid from version in delta generated message: $version");None
      case Right(v) => Some(v)
    }

  private def validateMessage(campaign: Campaign, from: String, to: String)
                             (implicit ec: ExecutionContext): DBIO[Unit] = {
    import scala.concurrent.duration._
    //this method only returns a DBIO so it can be used inside a for comprehension involving slick calls
    val fromR = parseVersion(from)
    val toR = parseVersion(to)
    if(fromR.isEmpty) {
      DBIO.failed(new IllegalArgumentException("Received GeneratedDelta message with invalid from version"))
    } else if(toR.isEmpty) {
      DBIO.failed(new IllegalArgumentException("Received GeneratedDelta message with invalid to version"))
    } else if(campaign.meta.deltaFrom.isEmpty) {
      DBIO.failed(new IllegalArgumentException("Received GeneratedDelta message for campaign without static delta"))
    } else if(campaign.meta.deltaFrom.get.version != fromR.get) {
      DBIO.failed(
        new IllegalArgumentException("Received GeneratedDelta message for campaign with differing from version"))
    } else if(campaign.meta.packageUuid.isEmpty) {
      DBIO.failed(new IllegalArgumentException("Received GeneratedDelta message for campaign without a target version"))
    } else {
      val f = async {
        val pkg = await(db.run(Packages.byUuid(campaign.meta.packageUuid.get.toJava)))
        if(pkg.id.version != toR.get) {
            DBIO.failed(new IllegalArgumentException(s"Version in GeneratedDelta message ($to) doesn't match version " +
              s"in campaign (${pkg.id.version})"))
        } else {
          DBIO.failed(new IllegalArgumentException(s"Failed to read package for campaign"))
        }
      }
      Await.result(f, 5.seconds)
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

  def deltaGenerationFailedAction(msg: GeneratingDeltaFailed)(implicit ec: ExecutionContext): Future[Done] = {
    db.run(Campaigns.setAsDraft(Campaign.Id(msg.id))).map(_ => Done)
  }
}
