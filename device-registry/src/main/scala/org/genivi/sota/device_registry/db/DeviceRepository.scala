/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.db

import java.time.Instant

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.data.{Device, DeviceT, Namespace, Uuid}
import org.genivi.sota.data.DeviceStatus.DeviceStatus
import org.genivi.sota.db.Operators.regex
import org.genivi.sota.db.SlickExtensions
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.refined.SlickRefined._
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext

object DeviceRepository extends SlickExtensions {

  import Device._
  import org.genivi.sota.db.SlickAnyVal._

  // scalastyle:off
  class DeviceTable(tag: Tag) extends Table[Device](tag, "Device") {
    def namespace = column[Namespace]("namespace")
    def uuid = column[Uuid]("uuid")
    def deviceName = column[DeviceName]("device_name")
    def deviceId = column[Option[DeviceId]]("device_id")
    def deviceType = column[DeviceType]("device_type")
    def lastSeen = column[Option[Instant]]("last_seen")
    def createdAt = column[Instant]("created_at")
    def activatedAt = column[Option[Instant]]("activated_at")
    def deviceStatus = column[DeviceStatus]("device_status")

    def * = (namespace, uuid, deviceName, deviceId, deviceType, lastSeen, createdAt, activatedAt, deviceStatus)
      .shaped <> ((Device.apply _).tupled, Device.unapply)

    def pk = primaryKey("uuid", uuid)
  }

  // scalastyle:on
  val devices = TableQuery[DeviceTable]

  def list(ns: Namespace, offset: Option[Long], limit: Option[Long]): DBIO[Seq[Device]] = {
    val filteredDevices = devices.filter(_.namespace === ns)
    (offset, limit) match {
      case (None, None) =>
        filteredDevices
          .sortBy(_.deviceName)
          .result
      case _ =>
        filteredDevices
          .defaultPaginateAndSort(_.deviceName, offset, limit)
          .result
    }
  }

  def create(ns: Namespace, device: DeviceT)
             (implicit ec: ExecutionContext): DBIO[Uuid] = {
    val uuid: Uuid = Uuid.generate()

    val dbIO = devices += Device(ns, uuid, device.deviceName, device.deviceId, device.deviceType,
                                 createdAt = Instant.now())

    dbIO
      .map(_ => uuid)
      .handleIntegrityErrors(Errors.ConflictingDevice)
      .transactionally
  }

  def findUuidFromUniqueDeviceIdOrCreate(ns: Namespace, deviceId: DeviceId, devT: DeviceT)
                                (implicit ec: ExecutionContext): DBIO[(Boolean, Uuid)] = for {
    devs <- findByDeviceId(ns, deviceId)
    (created, uuid) <- devs match {
      case Seq() => create(ns, devT).map((true, _))
      case Seq(d) => DBIO.successful((false, d.uuid))
      case _ => DBIO.failed(Errors.ConflictingDevice)
    }
  } yield (created, uuid)

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

  def findByGroupId(ns: Namespace, groupId: Uuid, offset: Option[Long], limit: Option[Long]): DBIO[Seq[Device]] = {
    val q = for {
      gm <- GroupMemberRepository.groupMembers if gm.groupId === groupId
      d <- devices if d.namespace === ns && gm.deviceUuid === d.uuid
    } yield d

    q.defaultPaginate(offset, limit).result
  }

  def search(ns: Namespace, re: String Refined Regex, offset: Option[Long], limit: Option[Long]): DBIO[Seq[Device]] = {
    val filteredDevices = devices.filter(d => d.namespace === ns && regex(d.deviceName, re))
    (offset, limit) match {
      case (None, None) =>
        filteredDevices
          .sortBy(_.deviceName)
          .result
      case _ =>
        filteredDevices
          .defaultPaginateAndSort(_.deviceName, offset, limit)
          .result
    }
  }

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

  def updateLastSeen(uuid: Uuid, when: Instant)
                    (implicit ec: ExecutionContext): DBIO[(Boolean, Namespace)] = {

    val sometime = Some(when)

    val dbIO = for {
      count <- devices.filter(_.uuid === uuid).filter(_.activatedAt.isEmpty).map(_.activatedAt).update(sometime)
      _ <- devices.filter(_.uuid === uuid).map(_.lastSeen).update(sometime)
      ns <- devices.filter(_.uuid === uuid).map(_.namespace).result.failIfNotSingle(Errors.MissingDevice)
    } yield (count > 0, ns)

    dbIO.transactionally
  }

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

  def countActivatedDevices(ns: Namespace, start: Instant, end: Instant): DBIO[Int] = {
    devices
      .filter(_.namespace === ns)
      .map(_.activatedAt.getOrElse(start.minusSeconds(36000)))
      .filter(activatedAt => activatedAt >= start && activatedAt < end)
      .countDistinct
      .result
  }

  def setDeviceStatus(uuid: Uuid, status: DeviceStatus)(implicit ec: ExecutionContext): DBIO[Unit] =
    devices
      .filter(_.uuid === uuid)
      .map(_.deviceStatus)
      .update(status)
      .handleSingleUpdateError(Errors.MissingDevice)
}
