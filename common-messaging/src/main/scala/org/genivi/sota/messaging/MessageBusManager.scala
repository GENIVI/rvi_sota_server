package org.genivi.sota.messaging

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import cats.data.Xor
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{Config, ConfigException}
import io.circe.{Decoder, Encoder}
import org.genivi.sota.messaging.Messages._
import org.genivi.sota.messaging.kinesis.KinesisClient
import org.genivi.sota.messaging.nats.NatsClient
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag
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
  def apply[T <: Message](fn: T => Unit): MessageBusPublisher[T] =
    new MessageBusPublisher[T] {
      override def publish(msg: T): Unit = fn(msg)
    }

  def ignore[T <: Message]: MessageBusPublisher[T] =
    new MessageBusPublisher[T] {
      override def publish(msg: T): Unit = ()
    }
}

object MessageBusManager {
  lazy val log = LoggerFactory.getLogger(this.getClass)

  def subscribe[T <: Message](system: ActorSystem, config: Config)
                             (implicit decoder: Decoder[T],
                              m: ClassTag[T])
      : ConfigException Xor Done = {
    config.getString("messaging.mode").toLowerCase().trim match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        log.trace(s"Using subject name: ${m.runtimeClass.streamName}")
        NatsClient.runListener(system, config, m.runtimeClass.streamName)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        log.trace(s"Using stream name: ${m.runtimeClass.streamName}")
        KinesisClient.runWorker(system, config, m.runtimeClass.streamName)
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.Right(Done)
      case _ => Xor.left(new Missing("Unknown messaging mode specified"))
    }
  }

  def getPublisher[T <: Message](system: ActorSystem, config: Config)
                                (implicit encoder: Encoder[T])
      : ConfigException Xor MessageBusPublisher[T] = {
    val log = Logging.getLogger(system, this.getClass)

    config.getString("messaging.mode").toLowerCase().trim match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        NatsClient.getPublisher[T](system, config)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        KinesisClient.getPublisher[T](system, config)
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.right(MessageBusPublisher(system.eventStream.publish))
      case _ => Xor.left(new Missing("Unknown messaging mode specified"))
    }
  }
}
