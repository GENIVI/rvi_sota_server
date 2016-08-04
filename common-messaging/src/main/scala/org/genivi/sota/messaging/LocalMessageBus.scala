/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.messaging

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import io.circe.Encoder
import org.genivi.sota.messaging.Messages.Message

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try

object LocalMessageBus {
  def subscribe[T <: Message](system: ActorSystem)(implicit m: ClassTag[T]): Source[T, NotUsed] = {
    Source.actorRef(MessageBus.DEFAULT_CLIENT_BUFFER_SIZE, OverflowStrategy.dropTail).mapMaterializedValue { ref =>
      system.eventStream.subscribe(ref, m.runtimeClass)
      NotUsed
    }
  }

  def publisher(system: ActorSystem): MessageBusPublisher = {
    new MessageBusPublisher {
      override def publish[T <: Message](msg: T)(implicit ex: ExecutionContext, encoder: Encoder[T]): Future[Unit] =
        Future.fromTry(Try(system.eventStream.publish(msg)))
    }
  }
}
