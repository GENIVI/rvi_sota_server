package org.genivi.sota.messaging.nats

import java.util.Properties

import akka.Done
import akka.actor.ActorSystem
import cats.data.Xor
import com.typesafe.config.{Config, ConfigException}
import io.circe.syntax._
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
      //Log connection info as displaying it in the exception is not possible.
      //TODO: Update to newer version of java_nats so we can rethrown ConnectException, See PRO-905
      log.debug("NATS connection properties: " + props.getProperty("servers"))
      val client = Conn.connect(props)
      system.registerOnTermination { client.close() }
      client
    }

  def getDeviceSeenPublisher(system: ActorSystem, config: Config): ConfigException Xor MessageBusPublisher[DeviceSeen] =
    getClient(system, config).map { conn =>
      MessageBusPublisher { msg =>
        conn.publish(msg.getClass.getName, msg.asJson.noSpaces)
      }
    }

  def getDeviceCreatedPublisher(system: ActorSystem,
                                config: Config): ConfigException Xor MessageBusPublisher[DeviceCreated] =
    getClient(system, config).map { conn =>
      MessageBusPublisher { msg => conn.publish(msg.getClass.getName, msg.asJson.noSpaces) }
    }

  def runListener(system: ActorSystem, config: Config, subjectName: String,
                  parseFn: String => io.circe.Error Xor Message)
      : ConfigException Xor Done =
    getClient(system, config).map { conn =>
      val subId =
        conn.subscribe(subjectName,
          (msg: Msg) =>
            parseFn(msg.body) match {
              case Xor.Right(m) =>
                system.eventStream.publish(m)
              case Xor.Left(ex) => log.error(s"invalid message received from message bus: ${msg.body}\n"
                + s"Got this parse error: ${ex.toString}")
            })
      system.registerOnTermination{ conn.unsubscribe(subId); conn.close() }
      Done
    }

  def runDeviceSeenListener(system: ActorSystem, config: Config): ConfigException Xor Done =
    runListener(system, config, DeviceSeen.getClass.getName, Messages.parseDeviceSeenMsg)

  def runDeviceCreatedListener(system: ActorSystem, config: Config): ConfigException Xor Done =
    runListener(system, config, DeviceCreated.getClass.getName, Messages.parseDeviceCreatedMsg)
}
