package org.genivi.sota.messaging

import java.time.Instant

import cats.data.Xor
import io.circe.{Decoder, Encoder}
import io.circe.parser._
import io.circe.generic.semiauto._
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.data.Device.{DeviceName, Id}
import org.genivi.sota.data.{Device, Namespace, PackageId}

import scala.concurrent.Future
import scala.reflect.ClassTag

object Messages {

  sealed trait Message

  val PartitionPrefix = 1

  final case class DeviceSeen(uuid: Id,
                              lastSeen: Instant) extends Message

  final case class DeviceCreated(namespace: Namespace,
                                 uuid: Id,
                                 deviceName: DeviceName,
                                 deviceId: Option[Device.DeviceId],
                                 deviceType: Device.DeviceType) extends Message

  final case class DeviceDeleted(namespace: Namespace, uuid: Id) extends Message

  final case class PackageCreated(namespace: Namespace, packageId: PackageId,
                                  description: Option[String], vendor: Option[String],
                                  signature: Option[String]) extends Message

  final case class PackageBlacklisted(namespace: Namespace, packageId: PackageId) extends Message


  //Create custom UpdateSpec here instead of using org.genivi.sota.core.data.UpdateSpec as that would require moving
  //multiple RVI messages into SotaCommon. Furthermore, for now this class contains just the info required by the
  //front end.
  final case class UpdateSpec(namespace: Namespace, deviceId: Device.Id, packageId: PackageId,
                              status: String) extends Message

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
    override def partitionKey(v: DeviceSeen): String =
      v.uuid.underlying.get.take(PartitionPrefix)

    implicit val encoder: Encoder[DeviceSeen] = deriveEncoder
    implicit val decoder: Decoder[DeviceSeen] = deriveDecoder
  }

  implicit val deviceCreatedMessageLike = new MessageLike[DeviceCreated] {
    override def partitionKey(v: DeviceCreated): String =
      v.uuid.underlying.get.take(PartitionPrefix)

    implicit val encoder: Encoder[DeviceCreated] = deriveEncoder
    implicit val decoder: Decoder[DeviceCreated] = deriveDecoder
  }

  implicit val deviceDeletedMessageLike = new MessageLike[DeviceDeleted] {
    override def partitionKey(v: DeviceDeleted): String =
      v.uuid.underlying.get.take(PartitionPrefix)

    implicit val encoder: Encoder[DeviceDeleted] = deriveEncoder
    implicit val decoder: Decoder[DeviceDeleted] = deriveDecoder
  }


  implicit val packageCreatedMessageLike = new MessageLike[PackageCreated] {
    override def partitionKey(v: PackageCreated): String = v.packageId.mkString

    implicit val encoder: Encoder[PackageCreated] = deriveEncoder
    implicit val decoder: Decoder[PackageCreated] = deriveDecoder
  }

  implicit val updateSpecMessageLike = new MessageLike[UpdateSpec] {
    override def partitionKey(v: UpdateSpec): String = v.deviceId.underlying.get

    implicit val encoder: Encoder[UpdateSpec] = deriveEncoder
    implicit val decoder: Decoder[UpdateSpec] = deriveDecoder
  }

  implicit val blacklistedPackageMessageLike = new MessageLike[PackageBlacklisted]() {
    override def partitionKey(v: PackageBlacklisted): String = v.packageId.mkString

    override implicit val encoder: Encoder[PackageBlacklisted] = deriveEncoder
    override implicit val decoder: Decoder[PackageBlacklisted] = deriveDecoder
  }
}
