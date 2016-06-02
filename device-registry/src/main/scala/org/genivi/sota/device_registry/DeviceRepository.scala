/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import cats.Show
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import java.util.UUID
import org.genivi.sota.data.{Device, DeviceT}
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.PackageId
import org.genivi.sota.db.Operators.regex
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.device_registry.common.DeviceRegistryErrors
import org.genivi.sota.refined.SlickRefined._
import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import slick.driver.MySQLDriver.api._


object Devices {

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
      _ <- notExists(ns, id, device.deviceName, device.deviceId)
      _ <- devices += Device(ns, id, device.deviceName, device.deviceId, device.deviceType)
    } yield id

    dbIO.transactionally
  }

  def notExists(ns: Namespace, id: Id, deviceName: DeviceName, deviceId: Option[DeviceId])
               (implicit ec: ExecutionContext): DBIO[Unit] = {
    devices
      .filter(_.namespace === ns)
      .filter(d => d.id === id || d.deviceName === deviceName || d.deviceId === deviceId)
      .map(_.id)
      .size
      .result
      .flatMap { count =>
        if(count > 0)
          DBIO.failed(DeviceRegistryErrors.ConflictingDevice)
        else
          DBIO.successful(())
      }
  }

  def exists(ns: Namespace, id: Id)
            (implicit ec: ExecutionContext): DBIO[Device] =
    devices
      .filter(d => d.namespace === ns && d.id === id)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[Device]](DBIO.failed(DeviceRegistryErrors.MissingDevice))(DBIO.successful))

  def findByDeviceName(ns: Namespace, deviceName: DeviceName)
                      (implicit ec: ExecutionContext): DBIO[Device] =
    devices
      .filter(d => d.namespace === ns && d.deviceName === deviceName)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[Device]](DBIO.failed(DeviceRegistryErrors.MissingDevice))(DBIO.successful))

  def findByDeviceId(ns: Namespace, deviceId: DeviceId)
                    (implicit ec: ExecutionContext): DBIO[Device] =
    devices
      .filter(d => d.namespace === ns && d.deviceId === deviceId)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[Device]](DBIO.failed(DeviceRegistryErrors.MissingDevice))(DBIO.successful))

  def search(ns: Namespace, re: String Refined Regex): DBIO[Seq[Device]] =
    devices
      .filter(d => d.namespace === ns && regex(d.deviceName, re))
      .result

  def update(ns: Namespace, id: Id, device: DeviceT)
            (implicit ec: ExecutionContext): DBIO[Unit] =
    device.deviceId match {
      case Some(deviceId) => for {
        _ <- exists(ns, id)
        _ <- findByDeviceId(ns, deviceId).asTry.flatMap { // negate result of action:
          case Success(_) => DBIO.failed(DeviceRegistryErrors.ConflictingDevice)
          case Failure(_) => DBIO.successful(())
        }
        _ <- devices.insertOrUpdate(Device(ns, id, device.deviceName, device.deviceId, device.deviceType))
      } yield ()
      case None => for {
        _ <- exists(ns, id)
        _ <- devices.insertOrUpdate(Device(ns, id, device.deviceName, device.deviceId, device.deviceType))
      } yield ()
    }

  def updateLastSeen(ns: Namespace, id: Id)
                    (implicit ec: ExecutionContext): DBIO[Unit] = for {
    device <- exists(ns, id)
    newDevice = device.copy(lastSeen = Some(Instant.now()))
    _ <- devices.insertOrUpdate(newDevice)
  } yield ()

  def delete(ns: Namespace, id: Id)
            (implicit ec: ExecutionContext): DBIO[Unit] = for {
    _ <- exists(ns, id)
    _ <- devices.filter(d => d.namespace === ns && d.id === id).delete
  } yield ()

}
