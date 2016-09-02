/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import java.time.Instant
import org.genivi.sota.core.data.DeviceStatus.DeviceStatus
import org.genivi.sota.core.data.UpdateStatus.UpdateStatus
import org.genivi.sota.core.db.UpdateSpecs
import org.genivi.sota.data.{CirceEnum, Device}
import org.genivi.sota.data.Namespace._
import org.genivi.sota.refined.SlickRefined._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import slick.driver.MySQLDriver.api._


object DeviceStatus extends CirceEnum {
  type DeviceStatus = Value

  val NotSeen, Error, UpToDate, Outdated = Value
}

case class DeviceUpdateStatus(device: Device.Id, status: DeviceStatus, lastSeen: Option[Instant])

object DeviceSearch {
  import UpdateSpecs._
  import DeviceStatus._

  lazy val logger = LoggerFactory.getLogger(this.getClass)

  def currentDeviceStatus(lastSeen: Option[Instant], updateStatuses: Seq[(Instant, UpdateStatus)]): DeviceStatus = {
    import UpdateStatus._

    if(lastSeen.isEmpty) {
      NotSeen
    } else {
      val statuses = updateStatuses.sortBy(_._1).reverse.map(_._2)

      if(statuses.headOption.contains(UpdateStatus.Failed)) {
        Error
      } else if(!statuses.forall(s => List(Canceled, Finished, Failed).contains(s))) {
        Outdated
      } else {
        UpToDate
      }
    }
  }


  def fetchDeviceStatus(searchR: Future[Seq[Device]])
                       (implicit db: Database, ec: ExecutionContext): Future[Seq[DeviceUpdateStatus]] = {
    val deviceLastSeens = searchR map deviceWithLastSeen

    deviceLastSeens flatMap { m =>
      val devicesWithDefault = m
        .map((deviceDefaultStatus _).tupled)
        .map { ds => (ds.device, ds) }
        .toMap

      val dbDevices =
        dbDeviceStatus(m) map { dbDeviceStatus =>
          val dbDevicesMap = dbDeviceStatus.map(dbD => (dbD.device, dbD)).toMap
          devicesWithDefault ++ dbDevicesMap
        } map(_.values.toSeq)

      db.run(dbDevices)
    }
  }

  protected def deviceDefaultStatus(device: Device.Id, lastSeen: Option[Instant]): DeviceUpdateStatus = {
    DeviceUpdateStatus(device, currentDeviceStatus(lastSeen, Seq.empty), lastSeen)
  }

  protected def deviceWithLastSeen(devices: Seq[Device]): Map[Device.Id, Option[Instant]] = {
    devices.map { device => (device.id, device.lastSeen) }.toMap
  }

  def dbDeviceStatus(devices: Map[Device.Id, Option[Instant]])
                    (implicit db: Database, ec: ExecutionContext): DBIO[Seq[DeviceUpdateStatus]] = {
    import org.genivi.sota.db.SlickExtensions._

    val updateSpecsByDevice =
      updateSpecs
        .map(us => (us.device, (us.creationTime, us.status)))
        .filter(_._1.inSet(devices.keys))
        .result

    updateSpecsByDevice.map {
      _.groupBy(_._1)
        .values
        .map { d => (d.head._1, d.map(_._2)) }
        .map { case (device, statuses) =>
          DeviceUpdateStatus(device, currentDeviceStatus(devices(device), statuses), devices(device))
        }.toSeq
    }
  }

}
