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

  def parseDeviceSeenMsg(json: String): io.circe.Error Xor DeviceSeen = {
    decode[DeviceSeen](json)
  }

  def parseDeviceCreatedMsg(json: String): io.circe.Error Xor DeviceCreated = {
    decode[DeviceCreated](json)
  }

  trait Message

  final case class DeviceSeen(deviceId: Device.Id,
                              lastSeen: Instant) extends Message

  final case class DeviceCreated(namespace: Namespace,
                                 deviceName: DeviceName,
                                 deviceId: Option[Device.DeviceId],
                                 deviceType: Device.DeviceType) extends Message


  object DeviceSeen {
    implicit val EncoderInstance: Encoder[DeviceSeen] = deriveEncoder
    implicit val DecoderInstance: Decoder[DeviceSeen] = deriveDecoder
  }

  object DeviceCreated {
    implicit val EncoderInstance: Encoder[DeviceCreated] = deriveEncoder
    implicit val DecoderInstance: Decoder[DeviceCreated] = deriveDecoder
  }
}
