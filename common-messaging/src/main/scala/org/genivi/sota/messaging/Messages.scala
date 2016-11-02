package org.genivi.sota.messaging

import java.time.Instant
import java.util.UUID

import cats.data.Xor
import cats.syntax.show._
import io.circe.{Decoder, Encoder}
import io.circe.parser._
import io.circe.generic.semiauto._
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.data.{Device, Namespace, PackageId, Uuid}

import scala.reflect.ClassTag

object Messages {

  import Device._

  sealed trait BusMessage

  val partitionPrefixSize = 256

  final case class DeviceSeen(namespace: Namespace,
                              uuid: Uuid,
                              lastSeen: Instant) extends BusMessage

  final case class DeviceCreated(namespace: Namespace,
                                 uuid: Uuid,
                                 deviceName: DeviceName,
                                 deviceId: Option[DeviceId],
                                 deviceType: DeviceType) extends BusMessage

  final case class DeviceDeleted(namespace: Namespace, uuid: Uuid) extends BusMessage

  final case class PackageCreated(namespace: Namespace, packageId: PackageId,
                                  description: Option[String], vendor: Option[String],
                                  signature: Option[String]) extends BusMessage

  final case class PackageBlacklisted(namespace: Namespace, packageId: PackageId) extends BusMessage


  //Create custom UpdateSpec here instead of using org.genivi.sota.core.data.UpdateSpec as that would require moving
  //multiple RVI messages into SotaCommon. Furthermore, for now this class contains just the info required by the
  //front end.
  final case class UpdateSpec(namespace: Namespace, device: Uuid, packageUuid: UUID,
                              status: String) extends BusMessage

  implicit class StreamNameOp[T <: Class[_]](v: T) {
    def streamName: String = {
      v.getSimpleName.filterNot(c => List('$').contains(c))
    }
  }

  implicit class StreamNameInstanceOp[T <: BusMessage](v: T) {
    def streamName: String = v.getClass.streamName
  }

  abstract class MessageLike[T]()(implicit val tag: ClassTag[T]) {
    def streamName: String = tag.runtimeClass.streamName

    def id(v: T): String

    def partitionKey(v: T): String = id(v).take(partitionPrefixSize)

    def parse(json: String): io.circe.Error Xor T = decode[T](json)

    implicit val encoder: Encoder[T]

    implicit val decoder: Decoder[T]
  }


  implicit val deviceSeenMessageLike = new MessageLike[DeviceSeen] {
    override def id(v: DeviceSeen): String = v.uuid.underlying.get

    implicit val encoder: Encoder[DeviceSeen] = deriveEncoder
    implicit val decoder: Decoder[DeviceSeen] = deriveDecoder
  }

  implicit val deviceCreatedMessageLike = new MessageLike[DeviceCreated] {
    override def id(v: DeviceCreated): String = v.uuid.underlying.get

    implicit val encoder: Encoder[DeviceCreated] = deriveEncoder
    implicit val decoder: Decoder[DeviceCreated] = deriveDecoder
  }

  implicit val deviceDeletedMessageLike = new MessageLike[DeviceDeleted] {
    override def id(v: DeviceDeleted): String = v.uuid.underlying.get

    implicit val encoder: Encoder[DeviceDeleted] = deriveEncoder
    implicit val decoder: Decoder[DeviceDeleted] = deriveDecoder
  }


  implicit val packageCreatedMessageLike = new MessageLike[PackageCreated] {
    override def id(v: PackageCreated): String = v.packageId.mkString

    implicit val encoder: Encoder[PackageCreated] = deriveEncoder
    implicit val decoder: Decoder[PackageCreated] = deriveDecoder
  }

  implicit val updateSpecMessageLike = new MessageLike[UpdateSpec] {
    override def id(v: UpdateSpec): String = v.device.show

    implicit val encoder: Encoder[UpdateSpec] = deriveEncoder
    implicit val decoder: Decoder[UpdateSpec] = deriveDecoder
  }

  implicit val blacklistedPackageMessageLike = new MessageLike[PackageBlacklisted]() {
    override def id(v: PackageBlacklisted): String = v.packageId.mkString

    override implicit val encoder: Encoder[PackageBlacklisted] = deriveEncoder
    override implicit val decoder: Decoder[PackageBlacklisted] = deriveDecoder
  }
}
