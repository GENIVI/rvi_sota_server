/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import java.util.UUID

import io.circe.{Decoder, Encoder}
import org.genivi.sota.core.data.UpdateStatus.UpdateStatus
import slick.driver.MySQLDriver.api._
import org.genivi.sota.core.data.VehicleStatus.VehicleStatus
import org.genivi.sota.core.db.UpdateSpecs
import org.genivi.sota.data.Vehicle
import org.joda.time.DateTime
import org.genivi.sota.refined.SlickRefined._

import scala.concurrent.ExecutionContext

object VehicleStatus extends Enumeration {
  type VehicleStatus = Value

  val NotSeen, Error, UpToDate, Outdated = Value

  implicit val encoder : Encoder[VehicleStatus] = Encoder[String].contramap(_.toString)
  implicit val decoder : Decoder[VehicleStatus] = Decoder[String].map(VehicleStatus.withName)
}

case class VehicleUpdateStatus(status: VehicleStatus, lastSeen: Option[DateTime])

object VehicleUpdateStatus {
  import UpdateSpecs._
  import VehicleStatus._

  def current(vehicle: Vehicle, updates: Seq[(UUID, UpdateStatus)]): VehicleUpdateStatus = {
    if(vehicle.lastSeen.isEmpty)
      VehicleUpdateStatus(VehicleStatus.NotSeen, None)
    else {
      val statuses = updates.map(_._2)

      if(statuses.contains(UpdateStatus.Failed))
        VehicleUpdateStatus(Error, vehicle.lastSeen)
      else if(!statuses.forall(_ == UpdateStatus.Finished))
        VehicleUpdateStatus(Outdated, vehicle.lastSeen)
      else
        VehicleUpdateStatus(UpToDate, vehicle.lastSeen)
    }
  }

  def findPackagesFor(vin: Vehicle.Vin)
                     (implicit db: Database, ec: ExecutionContext) : DBIO[Seq[(UUID, UpdateStatus)]] = {
    updateSpecs
      .filter(r => r.vin === vin)
      .map(us => (us.requestId, us.status))
      .result
  }

  def findPendingPackageIdsFor(vin: Vehicle.Vin)
                              (implicit db: Database, ec: ExecutionContext) : DBIO[Seq[UUID]] = {
    updateSpecs
      .filter(r => r.vin === vin)
      .filter(_.status.inSet(List(UpdateStatus.InFlight, UpdateStatus.Pending)))
      .map(_.requestId)
      .result
  }
}
