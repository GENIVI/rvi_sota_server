/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import java.time.Instant

import cats.data.Xor
import java.util.UUID

import org.genivi.sota.core.data.Campaign
import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.data.{Namespace, PackageId, Uuid}
import org.genivi.sota.refined.SlickRefined._

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._

object Campaigns {
  import org.genivi.sota.db.SlickExtensions._
  import Campaign._
  import SotaCoreErrors._

  // scalastyle:off
  class CampaignTable(tag: Tag) extends Table[CampaignMeta](tag, "Campaign") {
    def id = column[Campaign.Id]("uuid")
    def namespace = column[Namespace]("namespace")
    def name = column[String]("name")
    def launched = column[Boolean]("launched")
    def packageUuid = column[Option[UUID]]("package_uuid")
    def createdAt = column[Instant]("created_at")

    def pk = primaryKey("pk_campaign", id)

    def * = (id, namespace, name, launched, packageUuid, createdAt).shaped <>
      (x => CampaignMeta(x._1, x._2, x._3, x._4, x._5.map(Uuid.fromJava _), x._6)
      ,(x: CampaignMeta) => Some((x.id, x.namespace, x.name, x.launched, x.packageUuid.map(_.toJava), x.createdAt)))

    def uniqueName = index("Campaign_unique_name", (namespace, name), unique = true)
    def fkPkg = foreignKey("Campaign_pkg_fk", packageUuid, Packages.packages)(_.uuid.?)
  }
  // scalastyle:on

  // scalastyle:off
  class CampaignGroups(tag: Tag) extends Table[(Campaign.Id, CampaignGroup)](tag, "CampaignGroups") {
    def campaign = column[Campaign.Id]("campaign_id")
    def group = column[Uuid]("group_uuid")
    def update = column[Option[Uuid]]("update_request_id")

    def pk = primaryKey("pk_campaign_group", (campaign, group))

    def * = (campaign, group, update).shaped <>
      (x => (x._1, CampaignGroup(x._2, x._3))
      ,(x: (Campaign.Id, CampaignGroup)) => Some((x._1, x._2.group, x._2.updateRequest)))

  }
  // scalastyle:on

  val campaignsMeta = TableQuery[CampaignTable]
  val campaignsGroups = TableQuery[CampaignGroups]

  def byId(id: Campaign.Id): Query[CampaignTable, CampaignMeta, Seq]
    = campaignsMeta.filter(_.id === id)

  def canEdit(id: Campaign.Id)
             (implicit ec: ExecutionContext): DBIO[CampaignMeta] =
    byId(id)
      .filter (_.launched === false)
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
      .map(_.launched)
      .update(false)
      .handleSingleUpdateError(MissingCampaign)

  def setAsLaunch(id: Campaign.Id)
                 (implicit ec: ExecutionContext): DBIO[Campaign] = {
    val dbIO = for {
      camp <- fetch(id)
      newMeta <- camp.canLaunch() match {
        case Xor.Right(()) => DBIO.successful(camp.meta.copy(launched = true))
        case Xor.Left(err) => DBIO.failed(err)
      }
      _ <- campaignsMeta.insertOrUpdate(newMeta)
    } yield (camp.copy(meta = newMeta))

    dbIO.transactionally
  }

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
}
