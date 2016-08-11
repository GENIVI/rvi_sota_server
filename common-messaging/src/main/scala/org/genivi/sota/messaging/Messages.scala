package org.genivi.sota.messaging

import java.time.Instant

import cats.data.Xor
import io.circe.{Decoder, Encoder}
import io.circe.parser._
import io.circe.generic.semiauto._
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.data.Device.{DeviceName, Id}
import org.genivi.sota.data.{Device, Namespace, PackageId}

import scala.reflect.ClassTag

object Messages {

  sealed trait Message

  final case class DeviceSeen(deviceId: Device.Id,
                              lastSeen: Instant) extends Message

  final case class DeviceCreated(namespace: Namespace,
                                 deviceName: DeviceName,
                                 deviceId: Option[Device.DeviceId],
                                 deviceType: Device.DeviceType) extends Message

  final case class DeviceDeleted(ns: Namespace, id: Id) extends Message

  final case class PackageCreated(namespace: Namespace, pid: PackageId,
                                  description: Option[String], vendor: Option[String],
                                  signature: Option[String],
                                  fileName: String) extends Message

  implicit class StreamNameOp[T <: Class[_]](v: T) {
    def streamName: String = {
      v.getSimpleName.filterNot(c => List('$').contains(c))
    }
  }

  implicit class StreamNameInstanceOp[T <: Message](v: T) {
    def streamName: String = v.getClass.streamName
  }

  abstract class MessageLike[T]()(implicit val tag: ClassTag[T]) {
    def streamName: String = tag.runtimeClass.streamName

    def partitionKey(v: T): String

    def parse(json: String): io.circe.Error Xor T = decode[T](json)

    implicit val encoder: Encoder[T]

    implicit val decoder: Decoder[T]
  }


  implicit val deviceSeenMessageLike = new MessageLike[DeviceSeen] {
    override def partitionKey(v: DeviceSeen): String = v.deviceId.underlying.get

    implicit val encoder: Encoder[DeviceSeen] = deriveEncoder
    implicit val decoder: Decoder[DeviceSeen] = deriveDecoder
  }

  implicit val deviceCreatedMessageLike = new MessageLike[DeviceCreated] {
    override def partitionKey(v: DeviceCreated): String =
      v.deviceName.underlying

    implicit val encoder: Encoder[DeviceCreated] = deriveEncoder
    implicit val decoder: Decoder[DeviceCreated] = deriveDecoder
  }

  implicit val deviceDeletedMessageLike = new MessageLike[DeviceDeleted] {
    override def partitionKey(v: DeviceDeleted): String =
      v.id.underlying.get

    implicit val encoder: Encoder[DeviceDeleted] = deriveEncoder
    implicit val decoder: Decoder[DeviceDeleted] = deriveDecoder
  }


  implicit val packageCreatedMessageLike = new MessageLike[PackageCreated] {
    override def partitionKey(v: PackageCreated): String = v.pid.mkString

    implicit val encoder: Encoder[PackageCreated] = deriveEncoder
    implicit val decoder: Decoder[PackageCreated] = deriveDecoder
  }
}
