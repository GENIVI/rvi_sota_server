/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import cats.Show
import io.circe._
import java.time.Instant
import java.util.UUID

import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.data._
import slick.jdbc.MySQLProfile.api._
import Campaign._
import org.genivi.sota.core.data.CampaignStatus.Status

case class Campaign (meta: CampaignMeta, packageId: Option[PackageId], groups: Seq[CampaignGroup]) {
  def canLaunch(): Throwable Either Unit = {
    if (meta.packageUuid.isEmpty || groups.size < 1) {
      Left(SotaCoreErrors.CantLaunchCampaign)
    } else if (meta.status != CampaignStatus.Draft && meta.status != CampaignStatus.InPreparation) {
      Left(SotaCoreErrors.CampaignLaunched)
    } else {
      Right(())
    }
  }
}

sealed case class CampaignStatistics(groupId: Uuid, updateId: Uuid, deviceCount: Int, updatedDevices: Int,
                                     successfulUpdates: Int, failedUpdates: Int, cancelledUpdates: Int)

object CampaignStatus extends CirceEnum with SlickEnum {
  type Status = Value

  val Draft, InPreparation, Active = Value
}

object Campaign {
  case class CampaignMeta(
    id: Id,
    namespace: Namespace,
    name : String,
    status: Status = CampaignStatus.Draft,
    packageUuid: Option[Uuid] = None,
    createdAt: Instant,
    deltaFrom: Option[PackageId] = None,
    size: Option[Long] = None
  )
  case class CreateCampaign(name: String)
  case class SetCampaignGroups(groups: Seq[Uuid])
  case class CampaignGroup(group: Uuid, updateRequest: Option[Uuid])

  case class LaunchCampaignRequest(
    startDate: Option[Instant] = None,
    endDate: Option[Instant] = None,
    priority: Option[Int] = None,
    signature: Option[String] = None,
    description: Option[String] = None,
    requestConfirmation: Option[Boolean] = None
  ) {
    def isValid(): String Either Unit = (startDate, endDate) match {
      case (Some(sd), Some(ed)) =>
        if(sd.isBefore(ed)) {
          Right(())
        } else {
          Left("The LaunchCampaignRequest object is not valid because the start date is not before end date.")
        }
      case _ => Right(())
    }
  }

  final case class Id(underlying: Uuid)

  implicit val idDecoder: Decoder[Id] = Decoder[UUID].map(uuid => Id(Uuid.fromJava(uuid)))
  implicit val idEncoder: Encoder[Id] = Encoder[UUID].contramap(_.underlying.toJava)

  implicit val showId: Show[Id] = Show.show(id => Uuid.showUuid.show(id.underlying))

  implicit val idColumnType =
    MappedColumnType.base[Id, Uuid](_.underlying, Id(_))
}
