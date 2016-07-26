package org.genivi.sota.messaging

import java.time.Instant

import akka.Done
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


  def getSubscriber(system: ActorSystem, config: Config): ConfigException Xor Done = {
    val log = Logging.getLogger(system, this.getClass)
    config.getString("messaging.mode") match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        NatsClient.runListener(system, config)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        KinesisClient.runWorker(system, config, system.log)
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.Right(Done)
      case _ => throw new Missing("Unknown messaging mode specified")
    }
  }

  def getPublisher(system: ActorSystem, config: Config): ConfigException Xor (DeviceSeenMessage => Unit) = {
    val log = Logging.getLogger(system, this.getClass)
    config.getString("messaging.mode") match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        NatsClient.createPublisher(system, config)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        KinesisClient.createPublisher(system, config, system.log)
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.right((msg:DeviceSeenMessage) => system.eventStream.publish(msg))
      case _ => throw new Missing("Unknown messaging mode specified")
    }
  }
}
