/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core.campaigns

import cats.implicits._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.UpdateService
import org.genivi.sota.core.data.{Campaign, UpdateRequest}
import org.genivi.sota.core.db.{Campaigns, Packages, UpdateSpecs}
import org.genivi.sota.data._
import org.genivi.sota.messaging.Commit.ValidCommit
import org.genivi.sota.messaging.{MessageBusPublisher, Messages}
import org.genivi.sota.messaging.Messages.{CampaignLaunched, DeltaRequest, UriWithSimpleEncoding}
import org.slf4j.LoggerFactory
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object CampaignLauncher {

  import Campaign._
  import UpdateService._

  private val log = LoggerFactory.getLogger(this.getClass)

  def cancel(id: Campaign.Id, messageBus: MessageBusPublisher)
            (implicit db: Database, ec: ExecutionContext): Future[Unit] = {
    val dbIO = Campaigns.fetchGroups(id).flatMap { grps =>
      val cancelIO = grps.collect {
        case CampaignGroup(_, Some(ur)) =>
          UpdateSpecs.cancelAllUpdatesByRequest(ur).map(_ => ur)
      }

      DBIO.sequence(cancelIO)
    }

    val dbAct = dbIO.transactionally.flatMap{uuids => DBIO.sequence(uuids.map(UpdateSpecs.findPendingByRequest(_)))}
    db.run(dbAct).flatMap { res =>
      Future.traverse(res.flatten) { case (usr, namespace, packageUuid) =>
        messageBus.publishSafe(Messages.UpdateSpec(namespace, usr.device, packageUuid, UpdateStatus.Canceled))
      }
    }.map(_ => ())
  }

  def resolve(devices: Seq[Uuid]): DependencyResolver = { pkg =>
    Future.successful(devices.map(_ -> Set(pkg.id)).toMap)
  }

  def sendMsg(namespace: Namespace, devices: Set[Uuid], pkgId: PackageId, updateId: Uuid,
              messageBus: MessageBusPublisher)
             (implicit db: Database, ec: ExecutionContext)
      : Future[Unit] = {
    import scala.async.Async._
    async {
      val pkg = await(db.run(Packages.byId(namespace, pkgId)))
      val msg = CampaignLaunched(namespace, updateId, devices, UriWithSimpleEncoding(pkg.uri),
                                 pkgId, pkg.size, pkg.checkSum)
      await(messageBus.publish(msg))
    }
  }

  def launch(deviceRegistry: DeviceRegistry, updateService: UpdateService, id: Campaign.Id, lc: LaunchCampaignRequest,
              messageBus: MessageBusPublisher)
             (implicit db: Database, ec: ExecutionContext)
      : Future[Unit] = {
    def updateUpdateRequest(ur: UpdateRequest): UpdateRequest = {
      ur.copy(periodOfValidity = Interval(lc.startDate.getOrElse(ur.periodOfValidity.start),
                                          lc.endDate.getOrElse(ur.periodOfValidity.end)),
              priority = lc.priority.getOrElse(ur.priority),
              signature = lc.signature.getOrElse(ur.signature),
              description = lc.description,
              requestConfirmation = lc.requestConfirmation.getOrElse(ur.requestConfirmation)
      )
    }

    def launchGroup(ns: Namespace, pkgId: PackageId, campGrp: CampaignGroup,
                    messageBus: MessageBusPublisher): Future[Uuid] = {
      val groupId = campGrp.group
      for {
        updateRequest <- updateService.updateRequest(ns, pkgId).map(updateUpdateRequest)
        devices       <- deviceRegistry.fetchDevicesInGroup(ns, groupId)
        _             <- updateService.queueUpdate(ns, updateRequest, resolve(devices.values), messageBus)

        uuid          = Uuid.fromJava(updateRequest.id)
        _             <- db.run(Campaigns.setUpdateUuid(id, groupId, uuid))
        _             <- sendMsg(ns, devices.values.toSet, pkgId, uuid, messageBus)
      } yield uuid
    }

    for {
      camp <- db.run(Campaigns.fetch(id))
      _    <- camp.groups.toList.traverse(campGrp =>
                launchGroup(camp.meta.namespace, camp.packageId.get, campGrp, messageBus))
      _    <- db.run(Campaigns.setAsLaunch(id))
    } yield ()
  }

  /**
    * This method assumes that the target campaign's deltaFrom is not `None`
    */
  def generateDeltaRequest(id: Campaign.Id, lc: LaunchCampaignRequest, messageBus: MessageBusPublisher)
                          (implicit db: Database, ec: ExecutionContext) : Future[Unit] = {
    import MessageBusPublisher._
    import eu.timepit.refined._
    import org.genivi.sota.messaging.Commit._
    import Messages._

    def getCommits(from: PackageId.Version, to: PackageId.Version): DBIO[(Commit, Commit)] =
      refineV[ValidCommit](from.get) match {
        case Left(_) => DBIO.failed(new IllegalArgumentException(s"Delta from version is not a valid commit hash"))
        case Right(deltaFromVersion) => refineV[ValidCommit](to.get) match {
          case Left(_) => DBIO.failed(new IllegalArgumentException(s"campaign target version is not a valid " +
            s"commit hash"))
          case Right(targetVersion) => DBIO.successful((deltaFromVersion, targetVersion))
        }
      }

    val f = for {
      campaign   <- Campaigns.fetch(id)
      (from, to) <- getCommits(campaign.meta.deltaFrom.get.version, campaign.packageId.get.version)
      _          <- Campaigns.createLaunchCampaignRequest(id, lc)
      _          <- Campaigns.setAsInPreparation(id)
    } yield {
      DeltaRequest(id.underlying, campaign.meta.namespace, from, to)
    }

    db.run(f.transactionally).pipeToBus(messageBus)(identity).map(_ => ())
  }
}
