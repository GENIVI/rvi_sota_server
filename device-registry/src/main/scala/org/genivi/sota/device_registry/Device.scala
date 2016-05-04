/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry

import cats.Show
import cats.data.Xor
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.string._
import io.circe.{Decoder, Encoder}
import org.genivi.sota.data.Namespace._
import org.joda.time.DateTime
import slick.driver.MySQLDriver.MappedJdbcType
import slick.driver.MySQLDriver.api._

import Device._


trait CirceEnum extends Enumeration {
  implicit val encode: Encoder[Value] = Encoder[String].contramap(_.toString)
  implicit val decode: Decoder[Value] = Decoder[String].map(this.withName)
}

trait SlickEnum extends Enumeration {
  implicit val enumMapper = MappedJdbcType.base[Value, Int](_.id, this.apply)
}

final case class Device(namespace: Namespace,
                  id: Id,
                  deviceId: Option[DeviceId] = None,
                  deviceType: Device.DeviceType = DeviceType.Other,
                  lastSeen: Option[DateTime] = None)

object Device {

  // circe marshalling

  import io.circe._
  import org.genivi.sota.marshalling.CirceMarshallingSupport._

  implicit def idEncoder: Encoder[Id] = Encoder[String].contramap(implicitly[Show[Id]].show(_))
  implicit val idDecoder: Decoder[Id] = refinedDecoder[String, ValidId].map(Id(_))

  implicit def deviceIdEncoder: Encoder[DeviceId] = Encoder[String].contramap(implicitly[Show[DeviceId]].show(_))
  implicit val deviceIdDecoder: Decoder[DeviceId] = Decoder[String].map(DeviceId(_))

  //

  type ValidId = Uuid
  final case class Id(underlying: String Refined ValidId) extends AnyVal
  implicit val showId = new Show[Id] {
    def show(id: Id) = id.underlying.get
  }

  final case class DeviceId(underlying: String) extends AnyVal
  implicit val showDeviceId = new Show[DeviceId] {
    def show(deviceId: DeviceId) = deviceId.underlying
  }

  type DeviceType = DeviceType.DeviceType

  final object DeviceType extends CirceEnum with SlickEnum {
    type DeviceType = Value
    val Other, Vehicle = Value
  }
  implicit val showDeviceType = new Show[DeviceType.Value] {
    def show(dt: DeviceType.Value) = dt.toString
  }

  implicit val showDevice: Show[Device] =
    Show.show(d => d.deviceType match {
      case DeviceType.Vehicle => s"Vehicle(${d.id}, ${d.deviceId}, ${d.lastSeen})"
      case _ => s"Device(${d.id}, ${d.lastSeen})"
    })

  implicit val IdOrdering: Ordering[Id] = new Ordering[Id] {
    override def compare(id1: Id, id2: Id): Int = id1.underlying.get compare id2.underlying.get
  }

  implicit val DeviceIdOrdering: Ordering[DeviceId] = new Ordering[DeviceId] {
    override def compare(id1: DeviceId, id2: DeviceId): Int = id1.underlying compare id2.underlying
  }

  implicit def DeviceOrdering(implicit ord: Ordering[Id]): Ordering[Device] = new Ordering[Device] {
    override def compare(d1: Device, d2: Device): Int = ord.compare(d1.id, d2.id)
  }

}
