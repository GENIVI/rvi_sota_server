/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.data

import cats.Show
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import java.time.Instant
import org.genivi.sota.data.Namespace._
import slick.driver.MySQLDriver.api._

import Device._

/*
 * Device transfer object
 */
final case class DeviceT(
  deviceName: DeviceName,
  deviceId: Option[Device.DeviceId] = None,
  deviceType: Device.DeviceType = Device.DeviceType.Other
)


final case class Device(namespace: Namespace,
                  id: Id,
                  deviceName: DeviceName,
                  deviceId: Option[DeviceId] = None,
                  deviceType: Device.DeviceType = DeviceType.Other,
                  lastSeen: Option[Instant] = None)

object Device {

  type ValidId = Uuid
  final case class Id(underlying: String Refined ValidId) extends AnyVal
  implicit val showId = new Show[Id] {
    def show(id: Id) = id.underlying.get
  }

  final case class DeviceId(underlying: String) extends AnyVal
  implicit val showDeviceId = new Show[DeviceId] {
    def show(deviceId: DeviceId) = deviceId.underlying
  }

  final case class DeviceName(underlying: String) extends AnyVal
  implicit val showDeviceName = new Show[DeviceName] {
    def show(name: DeviceName) = name.underlying
  }

  type DeviceType = DeviceType.DeviceType

  final object DeviceType extends CirceEnum with SlickEnum {
    type DeviceType = Value
    val Other, Vehicle = Value
  }

  implicit val showDeviceType = Show.fromToString[DeviceType.Value]

  implicit val showDevice: Show[Device] = Show.show[Device] {
    case d if d.deviceType == DeviceType.Vehicle =>
      s"Vehicle: id=${d.id.underlying.get}, VIN=${d.deviceId}, lastSeen=${d.lastSeen}"
    case d => s"Device: id=${d.id.underlying.get}, lastSeen=${d.lastSeen}"
  }

  implicit val IdOrdering: Ordering[Id] = new Ordering[Id] {
    override def compare(id1: Id, id2: Id): Int = id1.underlying.get compare id2.underlying.get
  }

  implicit val DeviceIdOrdering: Ordering[DeviceId] = new Ordering[DeviceId] {
    override def compare(id1: DeviceId, id2: DeviceId): Int = id1.underlying compare id2.underlying
  }

  implicit def DeviceOrdering(implicit ord: Ordering[Id]): Ordering[Device] = new Ordering[Device] {
    override def compare(d1: Device, d2: Device): Int = ord.compare(d1.id, d2.id)
  }

  // Slick mapping

  implicit val idColumnType =
    MappedColumnType.base[Id, String](showId.show(_), (s: String) => Id(Refined.unsafeApply(s)))

}
