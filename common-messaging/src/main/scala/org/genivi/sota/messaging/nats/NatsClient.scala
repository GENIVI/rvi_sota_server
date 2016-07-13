package org.genivi.sota.messaging.nats

import java.net.ConnectException
import java.util.Properties

import akka.actor.ActorSystem
import cats.data.Xor
import com.typesafe.config.{Config, ConfigException}
import org.genivi.sota.messaging.ConfigHelpers._
import org.genivi.sota.messaging.Messages.DeviceSeenMessage
import org.genivi.sota.messaging.Messages
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
      //Log connection info as displaying it in the exception is not possible.
      log.debug("NATS connection properties: " + props.getProperty("servers"))
      val client = Conn.connect(props)
      system.registerOnTermination {
        client.close()
      }
      client
    }

  def createPublisher(system: ActorSystem, config: Config): ConfigException Xor (DeviceSeenMessage => Unit) =
    getClient(system, config).map(conn =>
          (msg: DeviceSeenMessage) => {
        import io.circe.syntax._
        import org.genivi.sota.marshalling.CirceInstances._
        conn.publish(msg.deviceId.underlying.get, msg.asJson.noSpaces)
    })

  def runListener(system: ActorSystem, config: Config): ConfigException Xor Unit =
    getClient(system, config).map { conn =>
        val subId =
          conn.subscribe("*",
                         (msg: Msg) =>
                           Messages.parseMsg(msg.body) match {
                             case Xor.Right(m) =>
                               system.eventStream.publish(m)
                             case Xor.Left(ex) => log.error(s"invalid message received from message bus: ${msg.body}\n"
                               + s"Got this parse error: ${ex.toString}")
                         })
        system.registerOnTermination{ conn.unsubscribe(subId); conn.close() }
    }
}
