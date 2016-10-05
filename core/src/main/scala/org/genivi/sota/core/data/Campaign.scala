/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import cats.Show
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import io.circe._
import io.circe.generic.auto._
import java.time.Instant
import java.util.UUID
import org.genivi.sota.data.{Namespace, PackageId, Uuid}
import slick.driver.MySQLDriver.api._

import Campaign._

case class Campaign (meta: CampaignMeta, packageId: Option[PackageId], groups: Seq[CampaignGroup]) {
  def canLaunch(): Boolean = meta.packageUuid.isDefined && groups.size > 0
}

object Campaign {
  case class CampaignMeta(
    id: Id,
    namespace: Namespace,
    name : String,
    launched: Boolean = false,
    packageUuid: Option[Uuid] = None
  )
  case class CreateCampaign(name: String)
  case class SetCampaignGroups(groups: Seq[Uuid])
  case class CampaignGroup(group: Uuid, updateRequest: Option[Uuid])

  final case class Id(underlying: Uuid)

  implicit val idDecoder: Decoder[Id] = Decoder[UUID].map(uuid => Id(Uuid.fromJava(uuid)))
  implicit val idEncoder: Encoder[Id] = Encoder[UUID].contramap(_.underlying.toJava)

  implicit val showId: Show[Id] = Show.show(id => Uuid.showUuid.show(id.underlying))

  implicit val idColumnType =
    MappedColumnType.base[Id, Uuid](_.underlying, Id(_))
}
