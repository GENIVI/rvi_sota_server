package org.genivi.sota.messaging

import java.time.Instant

import akka.actor.ActorSystem
import akka.event.Logging
import cats.data.Xor
import com.typesafe.config.ConfigException.Missing
import org.genivi.sota.data.Device
import com.typesafe.config.{Config, ConfigException}
import io.circe.Decoder
import io.circe.parser._
import org.genivi.sota.messaging.Messages.DeviceSeenMessage
import org.genivi.sota.messaging.kinesis.KinesisClient
import org.genivi.sota.messaging.nats.NatsClient

object Messages {

  def parseMsg(json: String): Xor[io.circe.Error, DeviceSeenMessage] = {
    decode[DeviceSeenMessage](json)
  }

  final case class DeviceSeenMessage(deviceId: Device.Id, lastSeen: Instant)

  object DeviceSeenMessage {
    import io.circe.Encoder
    import io.circe.generic.semiauto._
    import org.genivi.sota.marshalling.CirceInstances._

    implicit val EncoderInstance: Encoder[DeviceSeenMessage] = deriveEncoder
    implicit val DecoderInstance: Decoder[DeviceSeenMessage] = deriveDecoder
  }
}

object MessageBusManager {

  def start(system: ActorSystem, config: Config): ConfigException Xor (DeviceSeenMessage => Unit) = {
    val log = Logging.getLogger(system, this.getClass)
    config.getString("messaging.mode") match {
      case "nats" =>
        log.info("Starting NATS")
        NatsClient.runListener(system, config)
        NatsClient.createPublisher(system, config)
      case "kinesis" =>
        log.info("Starting Kinesis")
        KinesisClient.runWorker(system, config, system.log)
        KinesisClient.createPublisher(system, config, system.log)
      case "test" =>
        log.info("Starting Messaging in test mode")
        Xor.right((msg:DeviceSeenMessage) => system.eventStream.publish(msg))
      case _ => throw new Missing("Unknown messaging mode specified")
    }
  }
}
