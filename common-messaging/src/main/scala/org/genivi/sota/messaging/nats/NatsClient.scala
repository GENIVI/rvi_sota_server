package org.genivi.sota.messaging.nats

import java.util.Properties

import akka.NotUsed
import akka.actor.ActorSystem
import akka.actor.Status.Failure
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import cats.syntax.either._
import com.typesafe.config.Config
import io.circe.Decoder
import io.circe.syntax._
import io.circe.parser._
import org.genivi.sota.marshalling.CirceInstances._
import org.genivi.sota.messaging.ConfigHelpers._
import org.genivi.sota.messaging.Messages._
import org.genivi.sota.messaging.{MessageBus, MessageBusPublisher}
import org.nats.{Conn, Msg}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.Try
import scala.util.control.NoStackTrace

object NatsClient {
  val log = LoggerFactory.getLogger(this.getClass)

  case object NatsDisconnectedError extends Exception("Nats disconnected, unknown reason") with NoStackTrace

  private[this] def natsConfig(system: ActorSystem, config: Config): Throwable Either Properties = {
    for {
      natsConfig <- config.configAt("messaging.nats")
      userName <- natsConfig.readString("user")
      password <- natsConfig.readString("password")
      host <- natsConfig.readString("host")
      port <- natsConfig.readInt("port")
      props = new Properties()
      _ = props.put("servers", "nats://" + userName + ":" + password + "@" + host + ":" + port)
    } yield props
  }

  private[this] def connect(system: ActorSystem, props: Properties,
                            disconnectHandler: () => Any): Throwable Either Conn = {
    Either.catchNonFatal(Conn.connect(props, disconnHandler = _ => disconnectHandler.apply())).map { client =>
      system.registerOnTermination {
        client.close()
      }
      client
    }
  }

  def publisher(system: ActorSystem, config: Config): Throwable Either MessageBusPublisher = {
    for {
      connProps <- natsConfig(system, config)
      conn <- connect(system, connProps, () => throw NatsDisconnectedError)
    } yield
      new MessageBusPublisher {
        override def publish[T](msg: T)(implicit ex: ExecutionContext, messageLike: MessageLike[T]): Future[Unit] =
          Future {
            blocking {
              conn.publish(messageLike.streamName, msg.asJson(messageLike.encoder).noSpaces)
            }
          }
      }
  }

  def source[T](system: ActorSystem, config: Config, subjectName: String)
               (implicit decoder: Decoder[T]): Throwable Either Source[T, NotUsed] =
    natsConfig(system, config) map { connProps =>
      Source
        .actorRef[T] (MessageBus.DEFAULT_CLIENT_BUFFER_SIZE, OverflowStrategy.dropHead)
        .mapMaterializedValue { ref =>
          val disconnectHandler = { () => ref ! Failure(NatsDisconnectedError) }

          connect(system, connProps, disconnectHandler) match {
            case Right(conn) =>
              val subId = conn.subscribe(subjectName, (msg: Msg) => {
                decode[T](msg.body) match {
                  case Right(m) =>
                    ref ! m
                  case Left(ex) => log.error(s"invalid message received from message bus: ${msg.body}\n"
                    + s"Got this parse error: ${ex.toString}")
                }
              })

              (conn, subId)

            case Left(ex) =>
              throw ex
          }
        }
        .watchTermination() { case ((conn, subId), doneF) =>
          implicit val _ec = system.dispatcher
          doneF.andThen { case _ => Try { conn.unsubscribe(subId); conn.close() } }
          NotUsed
        }
    }
}
