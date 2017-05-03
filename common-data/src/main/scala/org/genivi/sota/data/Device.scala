/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.data

import java.time.{Instant, OffsetDateTime}

import cats.Show
import cats.syntax.show._
import org.genivi.sota.data.Device._
import org.genivi.sota.data.DeviceStatus._


/*
 * Device transfer object
 */
// TODO: Use org.genivi.sota.core.data.client.ResponseEncoder
final case class DeviceT(
  deviceName: DeviceName,
  deviceId: Option[Device.DeviceId] = None,
  deviceType: Device.DeviceType = Device.DeviceType.Other,
  credentials: Option[String] = None
)


final case class Device(namespace: Namespace,
                  uuid: Uuid,
                  deviceName: DeviceName,
                  deviceId: Option[DeviceId] = None,
                  deviceType: DeviceType = DeviceType.Other,
                  lastSeen: Option[Instant] = None,
                  createdAt: Instant,
                  activatedAt: Option[Instant] = None,
                  deviceStatus: DeviceStatus = NotSeen) {

  // TODO: Use org.genivi.sota.core.data.client.ResponseEncoder
  def toResponse: DeviceT = DeviceT(deviceName, deviceId, deviceType)
}

object Device {

  final case class DeviceId(underlying: String) extends AnyVal
  implicit val showDeviceId = new Show[DeviceId] {
    def show(deviceId: DeviceId) = deviceId.underlying
  }

  final case class DeviceName(underlying: String) extends AnyVal
  implicit val showDeviceName = new Show[DeviceName] {
    def show(name: DeviceName) = name.underlying
  }

  type DeviceType = DeviceType.DeviceType

  final object DeviceType extends CirceEnum {
    // TODO: We should encode Enums as strings, not Ints
    // Moved this from SlickEnum, because this should **NOT** be used
    // It's difficult to read this when reading from the database and the Id is not stable when we add/remove
    // values from the enum
    import slick.jdbc.MySQLProfile.MappedJdbcType
    import slick.jdbc.MySQLProfile.api._

    implicit val enumMapper = MappedJdbcType.base[Value, Int](_.id, this.apply)

    type DeviceType = Value
    val Other, Vehicle = Value
  }

  implicit val showDeviceType = Show.fromToString[DeviceType.Value]

  implicit val showDevice: Show[Device] = Show.show[Device] {
    case d if d.deviceType == DeviceType.Vehicle =>
      s"Vehicle: uuid=${d.uuid.show}, VIN=${d.deviceId}, lastSeen=${d.lastSeen}"
    case d => s"Device: uuid=${d.uuid.show}, lastSeen=${d.lastSeen}"
  }

  implicit val DeviceIdOrdering: Ordering[DeviceId] = new Ordering[DeviceId] {
    override def compare(id1: DeviceId, id2: DeviceId): Int = id1.underlying compare id2.underlying
  }

  implicit def DeviceOrdering(implicit ord: Ordering[Uuid]): Ordering[Device] = new Ordering[Device] {
    override def compare(d1: Device, d2: Device): Int = ord.compare(d1.uuid, d2.uuid)
  }

  implicit val showOffsetDateTable = new Show[OffsetDateTime] {
    def show(odt: OffsetDateTime) = odt.toString
  }

  case class ActiveDeviceCount(deviceCount: Int)
}
