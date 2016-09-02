/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import cats.Show
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.Json
import io.circe.jawn._
import java.util.UUID

import org.genivi.sota.data.{Device, DeviceT, Namespace}
import org.genivi.sota.data.Namespace._
import org.genivi.sota.db.Operators.regex
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.refined.SlickRefined._
import java.time.Instant

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import slick.driver.MySQLDriver.api._


object DeviceRepository {

  import Device._

  // TODO generalize
  implicit val deviceNameColumnType =
    MappedColumnType.base[DeviceName, String](
      { case DeviceName(value) => value.toString },
      DeviceName(_)
    )

  implicit val deviceIdColumnType =
    MappedColumnType.base[DeviceId, String](
      { case DeviceId(value) => value.toString },
      DeviceId(_)
    )

  // scalastyle:off
  class DeviceTable(tag: Tag) extends Table[Device](tag, "Device") {
    def namespace = column[Namespace]("namespace")
    def id = column[Id]("uuid")
    def deviceName = column[DeviceName]("device_name")
    def deviceId = column[Option[DeviceId]]("device_id")
    def deviceType = column[DeviceType]("device_type")
    def lastSeen = column[Option[Instant]]("last_seen")

    def * = (namespace, id, deviceName, deviceId, deviceType, lastSeen).shaped <>
      ((Device.apply _).tupled, Device.unapply)

    def pk = primaryKey("id", id)
  }

  // scalastyle:on
  val devices = TableQuery[DeviceTable]

  def list(ns: Namespace): DBIO[Seq[Device]] = devices.filter(_.namespace === ns).result

  def create(ns: Namespace, device: DeviceT)
             (implicit ec: ExecutionContext): DBIO[Id] = {
    val id: Id = Id(refineV[ValidId](UUID.randomUUID.toString).right.get)

    val dbIO = for {
      _ <- exists(ns, id).asTry.flatMap {
        case Success(_) => DBIO.failed(Errors.ConflictingDevice)
        case Failure(_) => DBIO.successful(())
      }
      _ <- notConflicts(ns, device.deviceName, device.deviceId)
      _ <- devices += Device(ns, id, device.deviceName, device.deviceId, device.deviceType)
    } yield id

    dbIO.transactionally
  }

  def notConflicts(ns: Namespace, deviceName: DeviceName, deviceId: Option[DeviceId])
                  (implicit ec: ExecutionContext): DBIO[Unit] = {
    devices
      .filter(_.namespace === ns)
      .filter(d => d.deviceName === deviceName || d.deviceId === deviceId)
      .exists
      .result
      .flatMap(if (_) DBIO.failed(Errors.ConflictingDevice) else DBIO.successful(()))
  }

  def exists(ns: Namespace, id: Id)
            (implicit ec: ExecutionContext): DBIO[Device] =
    devices
      .filter(d => d.namespace === ns && d.id === id)
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

  def update(ns: Namespace, id: Id, device: DeviceT)
            (implicit ec: ExecutionContext): DBIO[Unit] = {

    val dbIO = for {
      _ <- exists(ns, id)
      _ <- notConflicts(ns, device.deviceName, device.deviceId)
      _ <- devices.update(Device(ns, id, device.deviceName, device.deviceId, device.deviceType))
    } yield ()

    dbIO.transactionally
  }

  def findById(id: Device.Id)(implicit ec: ExecutionContext): DBIO[Device] = {
    devices
      .filter(_.id === id)
      .result
      .headOption
      .flatMap(_.fold[DBIO[Device]](DBIO.failed(Errors.MissingDevice))(DBIO.successful))
  }

  def updateLastSeen(id: Id)
                    (implicit ec: ExecutionContext): DBIO[Unit] = for {
    device <- findById(id)
    newDevice = device.copy(lastSeen = Some(Instant.now()))
    _ <- devices.insertOrUpdate(newDevice)
  } yield ()

  def delete(ns: Namespace, id: Id)
            (implicit ec: ExecutionContext): DBIO[Unit] = {
    val dbIO = for {
      _ <- exists(ns, id)
      _ <- devices.filter(d => d.namespace === ns && d.id === id).delete
      _ <- SystemInfo.delete(id)
    } yield ()

    dbIO.transactionally
  }

}
