/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import java.util.UUID
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.PackageId
import org.genivi.sota.db.Operators.regex
import org.genivi.sota.db.SlickExtensions._
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.refined.SlickRefined._
import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import slick.driver.MySQLDriver.api._


object Devices {

  import Device._

  implicit val idColumnType =
    MappedColumnType.base[Id, String](
      { case Id(value) => value.get },
      (s: String) => Id(refineV[ValidId](s).right.get)
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
    def deviceId = column[Option[DeviceId]]("device_id")
    def deviceType = column[DeviceType]("device_type")
    def lastSeen = column[Option[Instant]]("last_seen")

    def * = (namespace, id, deviceId, deviceType, lastSeen).shaped <> ((Device.apply _).tupled, Device.unapply)

    def pk = primaryKey("id", id)
  }
  // scalastyle:on

  val devices = TableQuery[DeviceTable]

  def list: DBIO[Seq[Device]] = devices.result

  def create (ns: Namespace, device: DeviceT)
             (implicit ec: ExecutionContext): DBIO[Id] = {
    val id: Id = Id(refineV[ValidId](UUID.randomUUID.toString).right.get)
    (devices += Device(ns, id, device.deviceId, device.deviceType)).map(_ => id)
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
                    (implicit ec: ExecutionContext): DBIO[Device] =
    devices
      .filter(d => d.namespace === ns && d.deviceId === deviceId)
      .result
      .headOption
      .flatMap(_.
        fold[DBIO[Device]](DBIO.failed(Errors.MissingDevice))(DBIO.successful))

  def search(ns: Namespace, re: String Refined Regex): DBIO[Seq[Device]] =
    devices
      .filter(d => d.namespace === ns && regex(d.deviceId, re))
      .result

  def update(ns: Namespace, id: Id, device: DeviceT)
            (implicit ec: ExecutionContext): DBIO[Unit] =
    device.deviceId match {
      case Some(deviceId) => for {
        _ <- exists(ns, id)
        _ <- findByDeviceId(ns, deviceId).asTry.flatMap { // negate result of action:
          case Success(_) => DBIO.failed(Errors.ConflictingDeviceId)
          case Failure(_) => DBIO.successful(())
        }
        _ <- devices.insertOrUpdate(Device(ns, id, device.deviceId, device.deviceType))
      } yield ()
      case None => for {
        _ <- exists(ns, id)
        _ <- devices.insertOrUpdate(Device(ns, id, device.deviceId, device.deviceType))
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
