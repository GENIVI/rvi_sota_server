package org.genivi.sota.messaging.nats

import java.util.Properties

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import cats.data.Xor
import com.typesafe.config.{Config, ConfigException}
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.circe.parser._
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.messaging.ConfigHelpers._
import org.genivi.sota.messaging.Messages._
import org.genivi.sota.messaging.{MessageBus, MessageBusPublisher, Messages}
import org.nats.{Conn, Msg}
import org.reactivestreams.{Publisher, Subscriber, Subscription}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, blocking}

object NatsClient {
  val log = LoggerFactory.getLogger(this.getClass)

  private[this] def fromConfig(system: ActorSystem, config: Config): ConfigException Xor Conn =
    for {
      natsConfig <- config.configAt("messaging.nats")
      userName   <- natsConfig.readString("user")
      password   <- natsConfig.readString("password")
      host       <- natsConfig.readString("host")
      port       <- natsConfig.readInt("port")
    } yield {
      val props = new Properties()
      props.put("servers", "nats://" + userName + ":" + password + "@" + host + ":" + port)
      val client = Conn.connect(props)
      system.registerOnTermination { client.close() }
      client
    }

  def publisher(system: ActorSystem, config: Config): ConfigException Xor MessageBusPublisher = {
    fromConfig(system, config) map { c =>
      new MessageBusPublisher {
        override def publish[T <: Message](msg: T)(implicit ec: ExecutionContext, encoder: Encoder[T]): Future[Unit] = {
          Future  { blocking { c.publish(msg.streamName, msg.asJson.noSpaces) } }
        }
      }
    }
  }

  def source[T <: Message](system: ActorSystem, config: Config, subjectName: String)
                          (implicit decoder: Decoder[T]): ConfigException Xor Source[T, NotUsed] =
  fromConfig(system, config).map { conn =>
    Source.actorRef[T](MessageBus.DEFAULT_CLIENT_BUFFER_SIZE,
      OverflowStrategy.dropHead).mapMaterializedValue { ref =>

        val subId = conn.subscribe(subjectName, (msg: Msg) =>
          decode[T](msg.body) match {
            case Xor.Right(m) =>
              ref ! m
            case Xor.Left(ex) => log.error(s"invalid message received from message bus: ${msg.body}\n"
              + s"Got this parse error: ${ex.toString}")
          })

        system.registerOnTermination{ conn.unsubscribe(subId); conn.close() }

        NotUsed
      }
  }
}
