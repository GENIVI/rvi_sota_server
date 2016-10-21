/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.db

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.data.{Device, DeviceT, Namespace, Uuid}
import org.genivi.sota.db.Operators.regex
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.messaging.Messages.DeviceSeen
import org.genivi.sota.refined.SlickRefined._
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext
import org.genivi.sota.db.Operators._


object DeviceRepository {

  import Device._

  // TODO generalize
  implicit val deviceNameColumnType =
    MappedColumnType.base[DeviceName, String](
      { case DeviceName(value) => value.toString },
      DeviceName
    )

  implicit val deviceIdColumnType =
    MappedColumnType.base[DeviceId, String](
      { case DeviceId(value) => value.toString },
      DeviceId
    )

  // scalastyle:off
  class DeviceTable(tag: Tag) extends Table[Device](tag, "Device") {
    def namespace = column[Namespace]("namespace")
    def uuid = column[Uuid]("uuid")
    def deviceName = column[DeviceName]("device_name")
    def deviceId = column[Option[DeviceId]]("device_id")
    def deviceType = column[DeviceType]("device_type")
    def lastSeen = column[Option[Instant]]("last_seen")

    def * = (namespace, uuid, deviceName, deviceId, deviceType, lastSeen).shaped <>
      ((Device.apply _).tupled, Device.unapply)

    def pk = primaryKey("uuid", uuid)
  }

  // scalastyle:on
  val devices = TableQuery[DeviceTable]

  def list(ns: Namespace): DBIO[Seq[Device]] = devices.filter(_.namespace === ns).result

  def create(ns: Namespace, device: DeviceT)
             (implicit ec: ExecutionContext): DBIO[Uuid] = {
    val uuid: Uuid = Uuid.generate()

    val dbIO = devices += Device(ns, uuid, device.deviceName, device.deviceId, device.deviceType)

    dbIO
      .map(_ => uuid)
      .handleIntegrityErrors(Errors.ConflictingDevice)
      .transactionally
  }

  def exists(ns: Namespace, uuid: Uuid)
            (implicit ec: ExecutionContext): DBIO[Device] =
    devices
      .filter(d => d.namespace === ns && d.uuid === uuid)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[Device]](DBIO.failed(Errors.MissingDevice))(DBIO.successful))

  def findByDeviceId(ns: Namespace, deviceId: DeviceId)
                    (implicit ec: ExecutionContext): DBIO[Seq[Device]] =
    devices
      .filter(d => d.namespace === ns && d.deviceId === deviceId)
      .result

  def search(ns: Namespace, re: String Refined Regex): DBIO[Seq[Device]] =
    devices
      .filter(d => d.namespace === ns && regex(d.deviceName, re))
      .result

  def update(ns: Namespace, uuid: Uuid, device: DeviceT)(implicit ec: ExecutionContext): DBIO[Unit] = {
    val dbIO = devices
      .filter(_.uuid === uuid)
      .map(r => (r.deviceName, r.deviceId, r.deviceType))
      .update((device.deviceName, device.deviceId, device.deviceType))
      .handleIntegrityErrors(Errors.ConflictingDevice)
      .handleSingleUpdateError(Errors.MissingDevice)

    dbIO.transactionally
  }

  def findByUuid(uuid: Uuid)(implicit ec: ExecutionContext): DBIO[Device] = {
    devices
      .filter(_.uuid === uuid)
      .result
      .headOption
      .flatMap(_.fold[DBIO[Device]](DBIO.failed(Errors.MissingDevice))(DBIO.successful))
  }

  def updateLastSeen(uuid: Uuid)
                    (implicit ec: ExecutionContext): DBIO[DeviceSeen] =
    for {
      device <- findByUuid(uuid)
      now = Instant.now()
      newDevice = device.copy(lastSeen = Some(now))
      _ <- devices.insertOrUpdate(newDevice)
    } yield DeviceSeen(device.namespace, uuid, now)

  def delete(ns: Namespace, uuid: Uuid)
            (implicit ec: ExecutionContext): DBIO[Unit] = {
    val dbIO = for {
      _ <- exists(ns, uuid)
      _ <- devices.filter(d => d.namespace === ns && d.uuid === uuid).delete
      _ <- SystemInfoRepository.delete(uuid)
    } yield ()

    dbIO.transactionally
  }

  def deviceNamespace(uuid: Uuid)(implicit ec: ExecutionContext): DBIO[Namespace] =
    devices
      .filter(_.uuid === uuid)
      .map(_.namespace)
      .result
      .failIfNotSingle(Errors.MissingDevice)
}
