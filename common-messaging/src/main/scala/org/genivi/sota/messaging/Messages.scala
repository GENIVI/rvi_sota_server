package org.genivi.sota.messaging

import java.time.Instant
import java.util.UUID

import cats.data.Xor
import cats.syntax.show._
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.{Decoder, Encoder}
import io.circe.parser._
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.data._
import org.genivi.sota.data.UpdateType.UpdateType
import shapeless.Lazy

import scala.reflect.ClassTag

object Messages {

  import Device._

  sealed trait BusMessage

  val partitionPrefixSize = 256

  final case class DeviceSeen(
    namespace: Namespace,
    uuid: Uuid,
    lastSeen: Instant) extends BusMessage

  final case class DeviceCreated(
    namespace: Namespace,
    uuid: Uuid,
    deviceName: DeviceName,
    deviceId: Option[DeviceId],
    deviceType: DeviceType) extends BusMessage

  final case class DeviceActivated(
    namespace: Namespace,
    uuid: Uuid,
    at: Instant) extends BusMessage

  final case class DeviceDeleted(
    namespace: Namespace,
    uuid: Uuid) extends BusMessage

  final case class PackageCreated(
    namespace: Namespace,
    packageId: PackageId,
    description: Option[String],
    vendor: Option[String],
    signature: Option[String]) extends BusMessage

  final case class PackageBlacklisted(
    namespace: Namespace,
    packageId: PackageId) extends BusMessage

  final case class ImageStorageUsage(namespace: Namespace, timestamp: Instant, byteCount: Long) extends BusMessage

  final case class PackageStorageUsage(namespace: Namespace, timestamp: Instant, byteCount: Long) extends BusMessage

  final case class BandwidthUsage(id: UUID, namespace: Namespace, timestamp: Instant, byteCount: Long,
                                  updateType: UpdateType, updateId: String) extends BusMessage

  //Create custom UpdateSpec here instead of using org.genivi.sota.core.data.UpdateSpec as that would require moving
  //multiple RVI messages into SotaCommon. Furthermore, for now this class contains just the info required by the
  //front end.
  final case class UpdateSpec(
    namespace: Namespace,
    device: Uuid,
    packageUuid: UUID,
    status: String) extends BusMessage

  final case class UserCreated(id: String, email: String) extends BusMessage

  final case class UserLogin(id: String, timestamp: Instant) extends BusMessage

  implicit class StreamNameOp[T <: Class[_]](v: T) {
    def streamName: String = {
      v.getSimpleName.filterNot(c => List('$').contains(c))
    }
  }

  implicit class StreamNameInstanceOp[T <: BusMessage](v: T) {
    def streamName: String = v.getClass.streamName
  }

  object MessageLike {
    def apply[T](idFn: T => String)
                (implicit ct: ClassTag[T],
                 encode: Lazy[DerivedObjectEncoder[T]],
                 decode: Lazy[DerivedDecoder[T]]): MessageLike[T] = new MessageLike[T] {
      override def id(v: T): String = idFn(v)

      import io.circe.generic.semiauto._

      override implicit val encoder: Encoder[T] = deriveEncoder[T]
      override implicit val decoder: Decoder[T] = deriveDecoder[T]
    }
  }

  abstract class MessageLike[T]()(implicit val tag: ClassTag[T]) {
    def streamName: String = tag.runtimeClass.streamName

    def id(v: T): String

    def partitionKey(v: T): String = id(v).take(partitionPrefixSize)

    def parse(json: String): io.circe.Error Xor T = decode[T](json)

    implicit val encoder: Encoder[T]

    implicit val decoder: Decoder[T]
  }

  implicit val deviceSeenMessageLike = MessageLike[DeviceSeen](_.uuid.show)

  implicit val deviceCreatedMessageLike = MessageLike[DeviceCreated](_.uuid.show)

  implicit val deviceDeletedMessageLike = MessageLike[DeviceDeleted](_.uuid.show)

  implicit val deviceActivatedMessageLike = MessageLike[DeviceActivated](_.uuid.show)

  implicit val packageCreatedMessageLike = MessageLike[PackageCreated](_.packageId.mkString)

  implicit val updateSpecMessageLike = MessageLike[UpdateSpec](_.device.show)

  implicit val blacklistedPackageMessageLike = MessageLike[PackageBlacklisted](_.packageId.mkString)

  implicit val imageStorageUsageMessageLike = MessageLike[ImageStorageUsage](_.namespace.get)

  implicit val packageStorageUsageMessageLike = MessageLike[PackageStorageUsage](_.namespace.get)

  implicit val bandwidthUsageMessageLike = MessageLike[BandwidthUsage](_.id.toString)

  implicit val userCreatedMessageLike = MessageLike[UserCreated](_.id)

  implicit val userLoginMessageLike = MessageLike[UserLogin](_.id)

}
