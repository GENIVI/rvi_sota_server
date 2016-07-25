package org.genivi.sota.messaging

import java.time.Instant

import cats.data.Xor
import io.circe.{Decoder, Encoder}
import io.circe.parser._
import io.circe.generic.semiauto._
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.data.Device.DeviceName
import org.genivi.sota.data.{Device, Namespace}

object Messages {

  def parseDeviceSeenMsg(json: String): io.circe.Error Xor DeviceSeenMessage = {
    decode[DeviceSeenMessage](json)
  }

  def parseDeviceCreatedMsg(json: String): io.circe.Error Xor DeviceCreatedMessage = {
    decode[DeviceCreatedMessage](json)
  }

  trait Message

  final case class DeviceSeenMessage(
                                      deviceId: Device.Id,
                                      lastSeen: Instant) extends Message

  final case class DeviceCreatedMessage(
                                         namespace: Namespace,
                                         deviceName: DeviceName,
                                         deviceId: Option[Device.DeviceId],
                                         deviceType: Device.DeviceType) extends Message


  object DeviceSeenMessage {
    implicit val EncoderInstance: Encoder[DeviceSeenMessage] = deriveEncoder
    implicit val DecoderInstance: Decoder[DeviceSeenMessage] = deriveDecoder
  }

  object DeviceCreatedMessage {
    implicit val EncoderInstance: Encoder[DeviceCreatedMessage] = deriveEncoder
    implicit val DecoderInstance: Decoder[DeviceCreatedMessage] = deriveDecoder
  }
}
