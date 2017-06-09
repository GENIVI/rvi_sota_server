/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.time.Instant

import java.util.UUID

import org.genivi.sota.core.data.{Campaign, CampaignStatus}
import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.core.data.CampaignStatus.Status
import org.genivi.sota.data.{Namespace, PackageId, Uuid}
import org.genivi.sota.refined.SlickRefined._

import scala.concurrent.ExecutionContext
import slick.jdbc.MySQLProfile.api._

object Campaigns {
  import org.genivi.sota.db.SlickExtensions._
  import Campaign._
  import SotaCoreErrors._

  private def optionPackageId(name: Option[PackageId.Name], version: Option[PackageId.Version]): Option[PackageId] =
    (name, version) match {
      case (Some(n), Some(v)) => Some(PackageId(n, v))
      case _ => None
    }

  // scalastyle:off
  class CampaignTable(tag: Tag) extends Table[CampaignMeta](tag, "Campaign") {
    def id = column[Campaign.Id]("uuid")
    def namespace = column[Namespace]("namespace")
    def name = column[String]("name")
    def status = column[Status]("status")
    def packageUuid = column[Option[UUID]]("package_uuid")
    def createdAt = column[Instant]("created_at")
    def deltaFromName = column[Option[PackageId.Name]]("delta_from_name")
    def deltaFromVersion = column[Option[PackageId.Version]]("delta_from_version")
    def size = column[Option[Long]]("size")

    def pk = primaryKey("pk_campaign", id)

    def * = (id, namespace, name, status, packageUuid, createdAt, deltaFromName, deltaFromVersion, size).shaped <>
      (x => CampaignMeta(x._1, x._2, x._3, x._4, x._5.map(Uuid.fromJava _), x._6, optionPackageId(x._7, x._8), x._9)
      ,(x: CampaignMeta) =>
        Some((x.id, x.namespace, x.name, x.status, x.packageUuid.map(_.toJava), x.createdAt,
          x.deltaFrom.map(_.name), x.deltaFrom.map(_.version), x.size)))

    def uniqueName = index("Campaign_unique_name", (namespace, name), unique = true)
    def fkPkg = foreignKey("Campaign_pkg_fk", packageUuid, Packages.packages)(_.uuid.?)
  }

  class CampaignGroups(tag: Tag) extends Table[(Campaign.Id, CampaignGroup)](tag, "CampaignGroups") {
    def campaign = column[Campaign.Id]("campaign_id")
    def group = column[Uuid]("group_uuid")
    def update = column[Option[Uuid]]("update_request_id")

    def pk = primaryKey("pk_campaign_group", (campaign, group))

    def * = (campaign, group, update).shaped <>
      (x => (x._1, CampaignGroup(x._2, x._3))
      ,(x: (Campaign.Id, CampaignGroup)) => Some((x._1, x._2.group, x._2.updateRequest)))

  }

  class LaunchCampaignRequests(tag: Tag)
    extends Table[(Campaign.Id, LaunchCampaignRequest)](tag, "LaunchCampaignRequests") {
    def campaign = column[Campaign.Id]("campaign_id", O.PrimaryKey)
    def startDate = column[Option[Instant]]("start_date")
    def endDate = column[Option[Instant]]("end_date")
    def priority = column[Option[Int]]("priority")
    def signature = column[Option[String]]("signature")
    def description = column[Option[String]]("description")
    def requestConfirmation = column[Option[Boolean]]("request_confirmation")

    def * = (campaign, startDate, endDate, priority, signature, description, requestConfirmation).shaped <>
      (x => (x._1, LaunchCampaignRequest(x._2, x._3, x._4, x._5, x._6, x._7)),
      (x: (Campaign.Id, LaunchCampaignRequest)) =>
        Some((x._1, x._2.startDate, x._2.endDate, x._2.priority, x._2.signature, x._2.description,
              x._2.requestConfirmation)))

  }
  // scalastyle:on

  val campaignsMeta = TableQuery[CampaignTable]
  val campaignsGroups = TableQuery[CampaignGroups]
  val launchCampaignRequests = TableQuery[LaunchCampaignRequests]

  def byId(id: Campaign.Id): Query[CampaignTable, CampaignMeta, Seq]
    = campaignsMeta.filter(_.id === id)

  def canEdit(id: Campaign.Id)
             (implicit ec: ExecutionContext): DBIO[CampaignMeta] =
    byId(id)
      .filter (_.status === CampaignStatus.Draft)
      .result
      .failIfNotSingle(CampaignLaunched)

  def create(ns: Namespace, name: String)
            (implicit ec: ExecutionContext): DBIO[Campaign.Id] = {
    val id: Campaign.Id = Id(Uuid.generate())

    (campaignsMeta += CampaignMeta(id, ns, name, createdAt = Instant.now()))
      .handleIntegrityErrors(ConflictingCampaign)
      .map(_ => id)
  }

  def delete(id: Campaign.Id)
            (implicit ec: ExecutionContext): DBIO[Unit] = {
    val dbIO = for {
      _ <- launchCampaignRequests.filter(_.campaign === id).delete
      _ <- campaignsMeta.filter(_.id === id).delete
      _ <- campaignsGroups.filter(_.campaign === id).delete
    } yield ()

    dbIO.transactionally
  }

  def fetch(id: Campaign.Id)
           (implicit ec: ExecutionContext): DBIO[Campaign] = {
    val dbIO = for {
      meta <- fetchMeta(id)
      pkg <- fetchPackage(meta)
      groups <- fetchGroups(id)
    } yield Campaign(meta, pkg, groups)

    dbIO.transactionally
  }

  def fetchGroups(id: Campaign.Id)
                 (implicit ec: ExecutionContext): DBIO[Seq[CampaignGroup]] =
    campaignsGroups
      .filter(_.campaign === id)
      .result
      .map (_.map(_._2))

  def fetchMeta(id: Campaign.Id)
               (implicit ec: ExecutionContext): DBIO[CampaignMeta] =
    byId(id)
      .result
      .failIfNotSingle(MissingCampaign)

  def fetchPackage(meta: CampaignMeta)
                  (implicit ec: ExecutionContext): DBIO[Option[PackageId]] =
    meta.packageUuid.fold[DBIO[Option[PackageId]]](DBIO.successful(None))( uuid =>
      Packages.byUuid(uuid.toJava).map(p => Some(p.id))
    )

  def list(ns: Namespace): DBIO[Seq[CampaignMeta]] = campaignsMeta.filter(_.namespace === ns).result

  def setAsDraft(id: Campaign.Id)
                (implicit ec: ExecutionContext): DBIO[Unit] =
    byId(id)
      .map(_.status)
      .update(CampaignStatus.Draft)
      .handleSingleUpdateError(MissingCampaign)

  def setAsLaunch(id: Campaign.Id)
                 (implicit ec: ExecutionContext): DBIO[Campaign] = {
    val dbIO = for {
      camp <- fetch(id)
      newMeta <- camp.canLaunch() match {
        case Right(()) => DBIO.successful(camp.meta.copy(status = CampaignStatus.Active))
        case Left(err) => DBIO.failed(err)
      }
      _ <- campaignsMeta.insertOrUpdate(newMeta)
    } yield camp.copy(meta = newMeta)

    dbIO.transactionally
  }

  def setAsInPreparation(id: Campaign.Id)
                (implicit ec: ExecutionContext): DBIO[Unit] =
    byId(id)
      .map(_.status)
      .update(CampaignStatus.InPreparation)
      .handleSingleUpdateError(MissingCampaign)

  def setGroups(id: Campaign.Id, groups: Seq[Uuid])
               (implicit ec: ExecutionContext): DBIO[Unit] = {
    val newGroups = groups.distinct.map( x => (id, CampaignGroup(x, None)))
    val dbIO = for {
      _ <- canEdit(id)
      _ <- campaignsGroups.filter(_.campaign === id).delete
      _ <- campaignsGroups ++= newGroups
    } yield ()

    dbIO.transactionally
  }

  def setName(id: Campaign.Id, name: String)
             (implicit ec: ExecutionContext): DBIO[Unit] =
    byId(id)
      .map(_.name)
      .update(name)
      .handleIntegrityErrors(ConflictingCampaign)
      .handleSingleUpdateError(MissingCampaign)

  def setUpdateUuid(id: Campaign.Id, group: Uuid, update: Uuid)
                 (implicit ec: ExecutionContext): DBIO[Unit] = {
    campaignsGroups
      .filter(x => x.campaign === id && x.group === group)
      .map(_.update)
      .update(Some(update))
      .handleSingleUpdateError(MissingCampaign)
      .map(_ => ())
  }

  def setPackage(id: Campaign.Id, pkgUuid: PackageId)
                (implicit ec: ExecutionContext): DBIO[Unit] = {
    val dbIO = for {
      meta <- canEdit(id)
      pkg <- Packages.find(meta.namespace, pkgUuid)
      _ <- byId(id)
             .map(_.packageUuid)
             .update(Some(pkg.uuid))
    } yield ()

    dbIO.transactionally
  }

  def setSize(id: Campaign.Id, size: Long)(implicit ec: ExecutionContext): DBIO[Unit] = {
    campaignsMeta
      .filter(_.id === id)
      .map(_.size)
      .update(Some(size))
      .handleSingleUpdateError(MissingCampaign)
  }

  def setDeltaFrom(id: Campaign.Id, deltaFrom: Option[PackageId])(implicit ec: ExecutionContext): DBIO[Unit] = {
    val dbIO = for {
      _    <- canEdit(id)
      cam  <- fetch(id)
      _    <- if(deltaFrom.isDefined && cam.packageId.isDefined && deltaFrom.get.version == cam.packageId.get.version) {
                DBIO.failed(InvalidDeltaFrom)
              } else {
                DBIO.successful(())
              }
      _    <- campaignsMeta
                .filter(_.id === id)
                .map(r => (r.deltaFromName, r.deltaFromVersion))
                .update((deltaFrom.map(_.name), deltaFrom.map(_.version)))
                .handleSingleUpdateError(MissingCampaign)
                .map(_ => ())
    } yield ()
    dbIO.transactionally
  }

  def createLaunchCampaignRequest(id: Campaign.Id, row: LaunchCampaignRequest)
            (implicit ec: ExecutionContext): DBIO[Unit] = {
    (launchCampaignRequests += ((id, row)))
      .handleIntegrityErrors(ConflictingLaunchCampaignRequest)
      .map(_ => ())
  }

  def fetchLaunchCampaignRequest(id: Campaign.Id)
           (implicit ec: ExecutionContext): DBIO[LaunchCampaignRequest] =
    launchCampaignRequests
      .filter(_.campaign === id)
      .result
      .failIfNotSingle(MissingLaunchCampaignRequest)
      .map(_._2)
}
