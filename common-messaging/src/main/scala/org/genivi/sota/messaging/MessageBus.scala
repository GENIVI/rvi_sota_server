package org.genivi.sota.messaging

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import cats.data.Xor
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{Config, ConfigException}
import io.circe.{Decoder, Encoder}
import org.genivi.sota.messaging.Messages._
import org.genivi.sota.messaging.kinesis.KinesisClient
import org.genivi.sota.messaging.nats.NatsClient
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait MessageBusPublisher {
  lazy private val logger = LoggerFactory.getLogger(this.getClass)

  def publish[T <: Message](msg: T)(implicit ex: ExecutionContext, encoder: Encoder[T]): Future[Unit]

  def publishSafe[T <: Message](msg: T)(implicit ec: ExecutionContext, encoder: Encoder[T]): Future[Try[Unit]] = {
    publish(msg)
      .map(r => Success(r))
      .recover { case t =>
        logger.error(s"Could not publish $msg msg to bus", t)
        Failure(t)
      }
  }
}

object MessageBusPublisher {
  def ignore = new MessageBusPublisher {
    override def publish[T <: Message](msg: T)(implicit ex: ExecutionContext, encoder: Encoder[T]): Future[Unit] =
      Future.successful(())
  }

  def apply(fn: Message => Unit): MessageBusPublisher = new MessageBusPublisher {
    override def publish[T <: Message](msg: T)(implicit ex: ExecutionContext, encoder: Encoder[T]): Future[Unit] =
      Future.successful(fn(msg))
  }
}


object MessageBus {
  val DEFAULT_CLIENT_BUFFER_SIZE = 1024 // number of msgs

  lazy val log = LoggerFactory.getLogger(this.getClass)

  import org.genivi.sota.marshalling.CirceInstances._

  def subscribe[T <: Message](system: ActorSystem, config: Config)
                             (implicit decoder: Decoder[T], m: ClassTag[T]): ConfigException Xor Source[T, NotUsed] = {
    config.getString("messaging.mode").toLowerCase().trim match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        log.trace(s"Using subject name: ${m.runtimeClass.streamName}")
        NatsClient.source(system, config, m.runtimeClass.streamName)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        log.trace(s"Using stream name: ${m.runtimeClass.streamName}")
        KinesisClient.source(system, config, m.runtimeClass.streamName)
      case "local" =>
        log.info("Using local event bus")
        Xor.right(LocalMessageBus.subscribe(system))
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.Right(LocalMessageBus.subscribe(system))
      case mode => Xor.left(new Missing(s"Unknown messaging mode specified ($mode)"))
    }
  }

  def publisher(system: ActorSystem, config: Config): ConfigException Xor MessageBusPublisher = {
    config.getString("messaging.mode").toLowerCase().trim match {
      case "nats" =>
        log.info("Starting messaging mode: NATS")
        NatsClient.publisher(system, config)
      case "kinesis" =>
        log.info("Starting messaging mode: Kinesis")
        KinesisClient.publisher(system, config)
      case "local" =>
        log.info("Using local message bus")
        Xor.right(LocalMessageBus.publisher(system))
      case "test" =>
        log.info("Starting messaging mode: Test")
        Xor.right(LocalMessageBus.publisher(system))
      case mode => Xor.left(new Missing(s"Unknown messaging mode specified ($mode)"))
    }
  }
}
