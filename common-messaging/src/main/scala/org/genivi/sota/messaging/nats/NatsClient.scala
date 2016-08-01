package org.genivi.sota.messaging.nats

import java.util.Properties

import akka.Done
import akka.actor.ActorSystem
import cats.data.Xor
import com.typesafe.config.{Config, ConfigException}
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.circe.parser._
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.messaging.ConfigHelpers._
import org.genivi.sota.messaging.Messages._
import org.genivi.sota.messaging.{MessageBusPublisher, Messages}
import org.nats.{Conn, Msg}
import org.slf4j.LoggerFactory

object NatsClient {
  val log = LoggerFactory.getLogger(this.getClass)

  private[this] def getClient(system: ActorSystem, config: Config): ConfigException Xor Conn =
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

  def getPublisher[T <: Message](system: ActorSystem, config: Config)
                                (implicit encoder: Encoder[T]): ConfigException Xor MessageBusPublisher[T] = {
    getClient(system, config).map { conn =>
      MessageBusPublisher { msg =>
        conn.publish(msg.streamName, msg.asJson.noSpaces)
      }
    }
  }

  def runListener[T <: Message](system: ActorSystem, config: Config, subjectName: String)
                               (implicit decoder: Decoder[T])
      : ConfigException Xor Done =
    getClient(system, config).map { conn =>
      val subId =
        conn.subscribe(subjectName,
          (msg: Msg) =>
            decode[T](msg.body) match {
              case Xor.Right(m) =>
                system.eventStream.publish(m)
              case Xor.Left(ex) => log.error(s"invalid message received from message bus: ${msg.body}\n"
                + s"Got this parse error: ${ex.toString}")
            })
      system.registerOnTermination{ conn.unsubscribe(subId); conn.close() }
      Done
    }
}
