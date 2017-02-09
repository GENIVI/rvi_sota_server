package org.genivi.sota.messaging.kafka

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.stream.scaladsl.Source
import cats.data.Xor
import com.typesafe.config.Config
import org.genivi.sota.messaging.Messages.{BusMessage, MessageLike}
import org.genivi.sota.messaging.daemon.MessageBusListenerActor

import scala.concurrent.Future

object MessageListener {

  type MsgParser[T <: BusMessage] = T => Future[_]

  def buildSource[T <: BusMessage](fromSource: Source[CommittableMessage[Array[Byte], T], NotUsed],
                                   op: MsgParser[T])
                                  (implicit system: ActorSystem, ml: MessageLike[T]): Source[T, NotUsed] = {
    implicit val ec = system.dispatcher

    fromSource
      .mapAsync(3) { commitableMsg =>
        val msg = commitableMsg.record.value()
        op(msg).map(_ => commitableMsg)
      }
      .mapAsync(1) { commitableMsg =>
        commitableMsg.committableOffset.commitScaladsl().map(_ => commitableMsg.record.value())
      }
  }

  private def kafkaSource[T <: BusMessage](config: Config)
                                          (implicit system: ActorSystem, ml: MessageLike[T])
  : Source[CommittableMessage[Array[Byte], T], NotUsed] =
    KafkaClient.commitableSource[T](config)(ml, system) match {
      case Xor.Right(s) => s
      case Xor.Left(err) => throw err
    }

  def props[T <: BusMessage](config: Config, op:MsgParser[T])
                            (implicit system: ActorSystem, ml: MessageLike[T]): Props = {
    val source = buildSource(kafkaSource(config), op)
    MessageBusListenerActor.props[T](source)(ml)
  }
}

