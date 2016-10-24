/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import cats.Show
import cats.data.Xor
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import io.circe._
import io.circe.generic.auto._
import java.time.Instant
import java.util.UUID
import org.genivi.sota.core.SotaCoreErrors
import org.genivi.sota.data.{Namespace, PackageId, Uuid}
import slick.driver.MySQLDriver.api._

import Campaign._

case class Campaign (meta: CampaignMeta, packageId: Option[PackageId], groups: Seq[CampaignGroup]) {
  def canLaunch(): Throwable Xor Unit = {
    if (!meta.packageUuid.isDefined || groups.size < 1) {
      Xor.Left(SotaCoreErrors.CantLaunchCampaign)
    } else if (meta.launched) {
      Xor.Left(SotaCoreErrors.CampaignLaunched)
    } else {
      Xor.Right(())
    }
  }
}

sealed case class CampaignStatistics(groupId: Uuid, updateId: Uuid, deviceCount: Int, updatedDevices: Int,
                                     successfulUpdates: Int, failedUpdates: Int)

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

  case class LaunchCampaign(
    startDate: Option[Instant] = None,
    endDate: Option[Instant] = None,
    priority: Option[Int] = None,
    signature: Option[String] = None,
    description: Option[String] = None,
    requestConfirmation: Option[Boolean] = None
  ) {
    def isValid(): String Xor Unit = (startDate, endDate) match {
      case (Some(sd), Some(ed)) =>
        if(sd.isBefore(ed)) {
          Xor.Right(())
        } else {
          Xor.Left("The LaunchCampaign object is not valid because the start date is not before end date.")
        }
      case _ => Xor.Right(())
    }
  }

  final case class Id(underlying: Uuid)

  implicit val idDecoder: Decoder[Id] = Decoder[UUID].map(uuid => Id(Uuid.fromJava(uuid)))
  implicit val idEncoder: Encoder[Id] = Encoder[UUID].contramap(_.underlying.toJava)

  implicit val showId: Show[Id] = Show.show(id => Uuid.showUuid.show(id.underlying))

  implicit val idColumnType =
    MappedColumnType.base[Id, Uuid](_.underlying, Id(_))
}
