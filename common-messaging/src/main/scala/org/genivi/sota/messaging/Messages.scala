package org.genivi.sota.messaging

import java.time.Instant

import cats.data.Xor
import io.circe.{Decoder, Encoder}
import io.circe.parser._
import io.circe.generic.semiauto._
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.data.Device.{DeviceName, Id}
import org.genivi.sota.data.{Device, Namespace, PackageId}

object Messages {

  def parseDeviceSeenMsg(json: String): io.circe.Error Xor DeviceSeen =
    decode[DeviceSeen](json)

  def parseDeviceCreatedMsg(json: String): io.circe.Error Xor DeviceCreated =
    decode[DeviceCreated](json)

  def parseDeviceDeletedMsg(json: String): io.circe.Error Xor DeviceDeleted =
    decode[DeviceDeleted](json)

  def parsePackageCreatedMsg(json: String): io.circe.Error Xor PackageCreated =
    decode[PackageCreated](json)

  sealed trait Message {
    def partitionKey: String
  }

  final case class DeviceSeen(deviceId: Device.Id,
                              lastSeen: Instant) extends Message {
    override val partitionKey = deviceId.underlying.get
  }

  final case class DeviceCreated(namespace: Namespace,
                                 deviceName: DeviceName,
                                 deviceId: Option[Device.DeviceId],
                                 deviceType: Device.DeviceType) extends Message {
    override val partitionKey = deviceName.underlying
  }

  final case class DeviceDeleted(ns: Namespace, id: Id) extends Message {
    override val partitionKey = id.underlying.get
  }

  final case class PackageCreated(ns: Namespace, pid: PackageId,
                   description: Option[String], vendor: Option[String],
                   signature: Option[String],
                   fileName: String) extends Message {
    override val partitionKey = pid.mkString
  }

  implicit class StreamNameOp[T](v: T) {
    def streamName: String = {
      v.getClass.getSimpleName.filterNot(c => List('$').contains(c))
    }
  }

  object DeviceSeen {
    implicit val EncoderInstance: Encoder[DeviceSeen] = deriveEncoder
    implicit val DecoderInstance: Decoder[DeviceSeen] = deriveDecoder
  }

  object DeviceCreated {
    implicit val EncoderInstance: Encoder[DeviceCreated] = deriveEncoder
    implicit val DecoderInstance: Decoder[DeviceCreated] = deriveDecoder
  }

  object DeviceDeleted {
    implicit val EncoderInstance: Encoder[DeviceDeleted] = deriveEncoder
    implicit val DecoderInstance: Decoder[DeviceDeleted] = deriveDecoder
  }

  object PackageCreated {
    implicit val EncoderInstance: Encoder[PackageCreated] = deriveEncoder
    implicit val DecoderInstance: Decoder[PackageCreated] = deriveDecoder
  }
}
