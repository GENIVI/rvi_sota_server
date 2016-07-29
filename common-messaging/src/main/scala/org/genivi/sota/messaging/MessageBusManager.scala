package org.genivi.sota.messaging

import akka.Done
import akka.actor.ActorSystem
import akka.event.{EventStream, Logging}
import cats.data.Xor
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{Config, ConfigException}
import org.genivi.sota.messaging.Messages._
import org.genivi.sota.messaging.kinesis.KinesisClient
import org.genivi.sota.messaging.nats.NatsClient
import org.slf4j.LoggerFactory

import scala.util.Try

trait MessageBusPublisher[T <: Message] {
  lazy private val logger = LoggerFactory.getLogger(this.getClass)

  def publish(msg: T): Unit

  def publishSafe(msg: T): Try[Unit] = {
    val t = Try(publish(msg))
    if(t.isFailure) {
      logger.error(s"Could not publish $msg msg to bus", t.failed.get)
    }
    t
  }
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
  lazy val log = LoggerFactory.getLogger(this.getClass)

  def subscribeDeviceSeen(system: ActorSystem, config: Config): ConfigException Xor Done = {
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

  def subscribeDeviceCreated(system: ActorSystem, config: Config): ConfigException Xor Done = {
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
                             config: Config): ConfigException Xor MessageBusPublisher[DeviceSeen] = {
    val log = Logging.getLogger(system, this.getClass)

    config.getString("messaging.mode") match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        NatsClient.getDeviceSeenPublisher(system, config)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        KinesisClient.getDeviceSeenPublisher(system, config)
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.right(MessageBusPublisher(system.eventStream.publish))
      case _ => Xor.left(new Missing("Unknown messaging mode specified"))
    }
  }

  def getDeviceCreatedPublisher(system: ActorSystem, config: Config)
      : ConfigException Xor MessageBusPublisher[DeviceCreated] = {
    val log = Logging.getLogger(system, this.getClass)

    config.getString("messaging.mode") match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        NatsClient.getDeviceCreatedPublisher(system, config)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        KinesisClient.getDeviceCreatedPublisher(system, config)
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.right(MessageBusPublisher(system.eventStream.publish))
      case _ => Xor.left(new Missing("Unknown messaging mode specified"))
    }

  }
}
