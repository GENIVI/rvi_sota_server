package org.genivi.sota.messaging

import java.time.Instant

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import cats.data.Xor
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{Config, ConfigException}
import org.genivi.sota.messaging.Messages.{DeviceCreatedMessage, DeviceSeenMessage}
import org.genivi.sota.messaging.kinesis.KinesisClient
import org.genivi.sota.messaging.nats.NatsClient

object MessageBusManager {


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
      case _ => throw new Missing("Unknown messaging mode specified")
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
      case _ => throw new Missing("Unknown messaging mode specified")
    }
  }

  def getDeviceSeenPublisher(system: ActorSystem, config: Config): ConfigException Xor (DeviceSeenMessage => Unit) = {
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
        Xor.right((msg:DeviceSeenMessage) => system.eventStream.publish(msg))
      case _ => throw new Missing("Unknown messaging mode specified")
    }
  }

  def getDeviceCreatedPublisher(system: ActorSystem, config: Config)
      : ConfigException Xor (DeviceCreatedMessage => Unit) = {
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
        Xor.right((msg:DeviceCreatedMessage) => system.eventStream.publish(msg))
      case _ => throw new Missing("Unknown messaging mode specified")
    }
  }
}
