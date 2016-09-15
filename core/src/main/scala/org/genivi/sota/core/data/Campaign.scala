/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import cats.Show
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import java.time.Instant
import java.util.UUID
import org.genivi.sota.data.{GroupInfo, Namespace, PackageId}
import slick.driver.MySQLDriver.api._

import Campaign._

case class Campaign (meta: CampaignMeta, packageId: Option[PackageId], groups: Seq[GroupInfo.Name]) {
  def canLaunch(): Boolean = meta.packageUuid.isDefined && groups.size > 0
}

object Campaign {
  case class CampaignMeta(
    id: Campaign.Id,
    namespace: Namespace,
    name : String,
    launched: Boolean = false,
    packageUuid: Option[UUID] = None
  )
  case class CreateCampaign(name: String)
  case class CampaignGroups(groups: Seq[GroupInfo.Name])

  type ValidId = Uuid
  case class Id(underlying: String Refined ValidId) extends AnyVal

  object ValidId {
    def from(uuid: UUID): Id = Id(Refined.unsafeApply(uuid.toString))
  }

  implicit val showId = new Show[Id] {
    def show(id: Id) = id.underlying.get
  }

  implicit val IdOrdering: Ordering[Id] = new Ordering[Id] {
    override def compare(id1: Id, id2: Id): Int = id1.underlying.get compare id2.underlying.get
  }

  implicit val idColumnType =
    MappedColumnType.base[Id, String](showId.show, (s: String) => Id(Refined.unsafeApply(s)))
}
