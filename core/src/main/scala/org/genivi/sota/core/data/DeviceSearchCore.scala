package org.genivi.sota.core.data

import java.time.Instant

import org.genivi.sota.data.{Device, DeviceUpdateStatus, Uuid}
import org.genivi.sota.data.DeviceSearchCommon._
import slick.jdbc.MySQLProfile.api._
import org.genivi.sota.core.db.UpdateSpecs._

import scala.concurrent.{ExecutionContext, Future}

object DeviceSearchCore {

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

  protected def deviceDefaultStatus(device: Uuid, lastSeen: Option[Instant]): DeviceUpdateStatus = {
    DeviceUpdateStatus(device, currentDeviceStatus(lastSeen, Seq.empty), lastSeen)
  }

  protected def deviceWithLastSeen(devices: Seq[Device]): Map[Uuid, Option[Instant]] = {
    devices.map { device => (device.uuid, device.lastSeen) }.toMap
  }

  def dbDeviceStatus(devices: Map[Uuid, Option[Instant]])
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
