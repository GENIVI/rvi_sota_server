package org.genivi.sota.messaging

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import cats.data.Xor
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{Config, ConfigException}
import org.genivi.sota.messaging.Messages.{DeviceCreatedMessage, DeviceSeenMessage, Message}
import org.genivi.sota.messaging.kinesis.KinesisClient
import org.genivi.sota.messaging.nats.NatsClient

trait MessageBusPublisher[-T <: Message] {
  def publish(msg: T)
}

object MessageBusPublisher {
  def apply[T <: Message](fn: T => Unit) = new MessageBusPublisher[T] {
    override def publish(msg: T): Unit = fn(msg)
  }

  def ignore[T <: Message] = new MessageBusPublisher[T] {
    override def publish(msg: T): Unit = ()
  }
}

object MessageBusManager {

  // TODO: All these can be made more general

  def getSubscriber(system: ActorSystem, config: Config): ConfigException Xor Done = {
    val log = Logging.getLogger(system, this.getClass)
    config.getString("messaging.mode") match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        NatsClient.runDeviceSeenListener(system, config)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        KinesisClient.runDeviceSeenWorker(system, config)
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.Right(Done)
      case _ => Xor.left(new Missing("Unknown messaging mode specified"))
    }
  }

  def getDeviceCreatedSubscriber(system: ActorSystem, config: Config): ConfigException Xor Done = {
    val log = Logging.getLogger(system, this.getClass)
    config.getString("messaging.mode") match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        NatsClient.runDeviceCreatedListener(system, config)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        KinesisClient.runDeviceCreatedWorker(system, config)
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.Right(Done)
      case _ => Xor.left(new Missing("Unknown messaging mode specified"))
    }
  }

  def getDeviceSeenPublisher(system: ActorSystem,
                             config: Config): ConfigException Xor MessageBusPublisher[DeviceSeenMessage] = {
    val log = Logging.getLogger(system, this.getClass)

    val fn = config.getString("messaging.mode") match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        NatsClient
          .getDeviceSeenPublisher(system, config)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        KinesisClient
          .getDeviceSeenPublisher(system, config)
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.right((msg:DeviceSeenMessage) => system.eventStream.publish(msg))
      case _ => Xor.left(new Missing("Unknown messaging mode specified"))
    }

    fn map(MessageBusPublisher(_))
  }

  def getDeviceCreatedPublisher(system: ActorSystem, config: Config)
      : ConfigException Xor MessageBusPublisher[DeviceCreatedMessage] = {
    val log = Logging.getLogger(system, this.getClass)

    val fn = config.getString("messaging.mode") match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        NatsClient.getDeviceCreatedPublisher(system, config)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        KinesisClient.getDeviceCreatedPublisher(system, config)
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.right((msg:DeviceCreatedMessage) => system.eventStream.publish(msg))
      case _ => Xor.left(new Missing("Unknown messaging mode specified"))
    }

    fn map(MessageBusPublisher(_))
  }
}
