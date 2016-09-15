/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import eu.timepit.refined.refineV
import java.time.Instant
import java.util.UUID
import org.genivi.sota.core.data.{Campaign, Package}
import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.data.{GroupInfo, Namespace, PackageId}
import org.genivi.sota.db.Operators._
import org.genivi.sota.refined.SlickRefined._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import slick.driver.MySQLDriver.api._

object Campaigns {
  import org.genivi.sota.db.SlickExtensions._
  import Campaign._
  import SotaCoreErrors._

  // scalastyle:off
  class CampaignTable(tag: Tag) extends Table[CampaignMeta](tag, "Campaign") {
    def id = column[Id]("uuid")
    def namespace = column[Namespace]("namespace")
    def name = column[String]("name")
    def launched = column[Boolean]("launched")
    def packageUuid = column[Option[UUID]]("package_uuid")

    def pk = primaryKey("pk_campaign", id)

    def * = (id, namespace, name, launched, packageUuid).shaped <>
      ((CampaignMeta.apply _).tupled, CampaignMeta.unapply)

    def uniqueName = index("Campaign_unique_name", (namespace, name), unique = true)
    def fkPkg = foreignKey("Campaign_pkg_fk", packageUuid, Packages.packages)(_.uuid)
  }

  class CampaignGroups(tag: Tag) extends Table[(Campaign.Id, GroupInfo.Name)](tag, "CampaignGroups") {
    def campaignId = column[Campaign.Id]("campaign_id")
    def group = column[GroupInfo.Name]("group_name")

    def pk = primaryKey("pk_campaign_group", (campaignId, group))

    def * = (campaignId, group)

  }
  // scalastyle:on

  val campaignsMeta = TableQuery[CampaignTable]
  val campaignsGroups = TableQuery[CampaignGroups]

  def byId(id: Id): Query[CampaignTable, CampaignMeta, Seq]
    = campaignsMeta.filter(_.id === id)

  def canEdit(id: Id)
             (implicit ec: ExecutionContext): DBIO[CampaignMeta] =
    byId(id)
      .filter (_.launched === false)
      .result
      .failIfNotSingle(CampaignLaunched)

  def create(ns: Namespace, name: String)
            (implicit ec: ExecutionContext): DBIO[Campaign.Id] = {
    val id: Id = Id(refineV[ValidId](UUID.randomUUID.toString).right.get)

    (campaignsMeta += CampaignMeta(id, ns, name))
      .handleIntegrityErrors(ConflictingCampaign)
      .map(_ => id)
  }

  def delete(id: Id)
            (implicit ec: ExecutionContext): DBIO[Unit] = {
    val dbIO = for {
      _ <- campaignsMeta.filter(_.id === id).delete
      _ <- campaignsGroups.filter(_.campaignId === id).delete
    } yield ()

    dbIO.transactionally
  }

  def exists(ns: Namespace, id: Id)
            (implicit ec: ExecutionContext): DBIO[CampaignMeta] =
    campaignsMeta
      .filter(c => c.namespace === ns && c.id === id)
      .result
      .failIfNotSingle(MissingCampaign)


  def fetch(id: Id)
           (implicit ec: ExecutionContext): DBIO[Campaign] = {
    val dbIO = for {
      meta <- fetchMeta(id)
      pkg <- fetchPackage(meta)
      groups <- fetchGroups(id)
    } yield Campaign(meta, pkg, groups)

    dbIO.transactionally
  }

  def fetchGroups(id: Id)
                 (implicit ec: ExecutionContext): DBIO[Seq[GroupInfo.Name]] =
    campaignsGroups
      .filter(_.campaignId === id)
      .result
      .map (_.map(_._2))

  def fetchMeta(id: Id)
               (implicit ec: ExecutionContext): DBIO[CampaignMeta] =
    byId(id)
      .result
      .failIfNotSingle(MissingCampaign)

  def fetchPackage(meta: CampaignMeta)
                  (implicit ec: ExecutionContext): DBIO[Option[PackageId]] =
    meta.packageUuid.fold[DBIO[Option[PackageId]]](DBIO.successful(None))( uuid =>
      Packages.byUuid(uuid).map(p => Some(p.id))
    )

  def list(ns: Namespace): DBIO[Seq[CampaignMeta]] = campaignsMeta.filter(_.namespace === ns).result

  def setAsDraft(id:Id)
                (implicit ec: ExecutionContext): DBIO[Unit] =
    byId(id)
      .map(_.launched)
      .update(false)
      .handleSingleUpdateError(MissingCampaign)

  def setAsLaunch(id: Id)
                 (implicit ec: ExecutionContext): DBIO[Unit] = {
    val dbIO = for {
      meta <- fetchMeta(id)
      groups <- fetchGroups(id)
      newMeta <- if (Campaign(meta, None, groups).canLaunch()) {
          DBIO.successful(meta.copy(launched = true))
        } else { DBIO.failed(SotaCoreErrors.CantLaunchCampaign) }
      _ <- campaignsMeta.insertOrUpdate(newMeta)
    } yield ()

    dbIO.transactionally
  }

  def setGroups(id: Id, groups: Seq[GroupInfo.Name])
               (implicit ec: ExecutionContext): DBIO[Unit] = {
    val newGroups = groups.map((id, _))
    val dbIO = for {
      _ <- canEdit(id)
      _ <- campaignsGroups.filter(_.campaignId === id).delete
      _ <- campaignsGroups ++= newGroups
    } yield ()

    dbIO.transactionally
  }

  def setName(id: Id, name: String)
             (implicit ec: ExecutionContext): DBIO[Unit] = {
    val dbIO = for {
      _ <- canEdit(id)
      _ <- byId(id)
             .map(_.name)
             .update(name)
             .handleIntegrityErrors(ConflictingCampaign)
    } yield ()

    dbIO.transactionally
  }

  def setPackage(id: Id, pkgId: PackageId)
                (implicit ec: ExecutionContext): DBIO[Unit] = {
    val dbIO = for {
      meta <- canEdit(id)
      pkg <- Packages.find(meta.namespace, pkgId)
      _ <- byId(id)
             .map(_.packageUuid)
             .update(Some(pkg.uuid))
    } yield ()

    dbIO.transactionally
  }
}
