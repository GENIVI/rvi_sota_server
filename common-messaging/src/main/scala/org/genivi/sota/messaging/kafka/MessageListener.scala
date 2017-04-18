package org.genivi.sota.messaging.kafka

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import org.genivi.sota.messaging.MessageBus
import org.genivi.sota.messaging.Messages.{BusMessage, MessageLike}
import org.genivi.sota.messaging.daemon.MessageBusListenerActor

import scala.concurrent.{ExecutionContext, Future}

object MessageListener {

  type MsgParser[T <: BusMessage] = T => Future[_]

  /**
    * This trait is used to enable NATS to pretend it can do committable messages
    */
  trait CommittableMsg[T <: BusMessage] {
    def msg(): T
    def commitRecord()(implicit ec: ExecutionContext): Future[T]
  }

  class NatsMsg[T <: BusMessage](message: T) extends CommittableMsg[T] {
    override def msg(): T = message

    override def commitRecord()(implicit ec: ExecutionContext): Future[T] = Future.successful(msg)
  }

  class KafkaMsg[T <: BusMessage](message: CommittableMessage[_, T]) extends CommittableMsg[T] {
    override def msg(): T = message.record.value()

    override def commitRecord()(implicit ec: ExecutionContext): Future[T] =
      message.committableOffset.commitScaladsl().map(_ => msg())
  }

  def buildSource[T <: BusMessage](fromSource: Source[CommittableMsg[T], NotUsed],
                                   op: MsgParser[T])
                                  (implicit system: ActorSystem, ml: MessageLike[T]): Source[T, NotUsed] = {
    implicit val ec = system.dispatcher

    fromSource
      .mapAsync(3) { committableMsg =>
        val msg = committableMsg.msg()
        op(msg).map(_ => committableMsg)
      }
      .mapAsync(1) { committableMsg =>
        committableMsg.commitRecord()
      }
  }

  def props[T <: BusMessage](config: Config, op:MsgParser[T])
                            (implicit system: ActorSystem, ml: MessageLike[T]): Props = {
    val source = buildSource(MessageBus.subscribeCommittable(config), op)
    MessageBusListenerActor.props[T](source)(ml)
  }
}
