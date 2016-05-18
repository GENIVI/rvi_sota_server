/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import io.circe.{Decoder, Encoder, Json}
import org.genivi.sota.core.data.UpdateStatus.UpdateStatus
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.data.VehicleStatus.VehicleStatus
import org.genivi.sota.core.db.UpdateSpecs
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.Vehicle
import org.joda.time.DateTime
import org.genivi.sota.refined.SlickRefined._
import io.circe.syntax._
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import scala.concurrent.ExecutionContext


object VehicleStatus extends Enumeration {
  type VehicleStatus = Value

  val NotSeen, Error, UpToDate, Outdated = Value

  implicit val encoder : Encoder[VehicleStatus] = Encoder[String].contramap(_.toString)
  implicit val decoder : Decoder[VehicleStatus] = Decoder[String].map(VehicleStatus.withName)
}

case class VehicleUpdateStatus(vin: Vehicle.Vin, status: VehicleStatus, lastSeen: Option[DateTime])

object VehicleSearch {
  import UpdateSpecs._
  import VehicleStatus._

  import org.genivi.sota.db.SlickExtensions.jodaDateTimeMapping

  def currentVehicleStatus(lastSeen: Option[DateTime], updateStatuses: Seq[UpdateStatus]): VehicleStatus =
    if(lastSeen.isEmpty) {
      VehicleStatus.NotSeen
    } else {
      if(updateStatuses.contains(UpdateStatus.Failed)) {
        Error
      } else if(!updateStatuses.forall(_ == UpdateStatus.Finished)) {
        Outdated
      } else {
        UpToDate
      }
    }

  def vinsWithStatus (ns: Namespace)
                     (implicit db: Database, ec: ExecutionContext): DBIO[Seq[VehicleUpdateStatus]] = {
    val updateSpecsByVin = updateSpecs.map(us => (us.vin, us.status))

    val updateStatusByVin = updateSpecs.filter(_.namespace === ns).map(_.vin)
      .joinLeft(updateSpecsByVin).on(_ === _._1)
      .map { case (vin, statuses) => (vin, statuses.map(_._2)) }
      .result

    updateStatusByVin.map {
      _.groupBy(_._1)
        .values
        .map { v => (v.head._1, v.flatMap(_._2)) }
        .map { case (vin, statuses) =>
          VehicleUpdateStatus(vin, currentVehicleStatus(None, statuses), None)

        }.toSeq
    }
  }
}
