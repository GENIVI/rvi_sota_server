/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.data

import cats.Show
import cats.data.Xor
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.string._
import org.genivi.sota.data.Namespace._
import org.joda.time.DateTime

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
                        lastSeen: Option[DateTime] = None)

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
    case d if d.deviceType == DeviceType.Vehicle => s"Vehicle(${d.id}, ${d.deviceId}, ${d.lastSeen})"
    case d => s"Device(${d.id}, ${d.lastSeen})"
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

  // Circe encoding

  import io.circe._
  import org.genivi.sota.marshalling.CirceRefined._

  implicit val idEncoder: Encoder[Id] = Encoder[String].contramap(implicitly[Show[Id]].show(_))
  implicit val idDecoder: Decoder[Id] = refinedDecoder[String, ValidId].map(Id(_))

  // TODO generalize
  implicit val deviceNameEncoder: Encoder[DeviceName] = Encoder[String].contramap(implicitly[Show[DeviceName]].show(_))
  implicit val deviceNameDecoder: Decoder[DeviceName] = Decoder[String].map(DeviceName(_))

  implicit val deviceIdEncoder: Encoder[DeviceId] = Encoder[String].contramap(implicitly[Show[DeviceId]].show(_))
  implicit val deviceIdDecoder: Decoder[DeviceId] = Decoder[String].map(DeviceId(_))

}
