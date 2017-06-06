package org.genivi.sota.messaging.daemon

import akka.{Done, NotUsed}
import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.genivi.sota.messaging.Messages.MessageLike
import org.genivi.sota.messaging.daemon.MessageBusListenerActor.Subscribe

import scala.concurrent.duration._
import scala.util.Try
import akka.pattern.pipe

class MessageBusListenerActor[M](source: Source[M, NotUsed])(implicit messageLike: MessageLike[M])
  extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

  override def postRestart(reason: Throwable): Unit = trySubscribeDelayed()

  private def subscribed: Receive = {
    log.info(s"Subscribed to ${messageLike.streamName}")

    {
      case Failure(ex) =>
        log.error(ex, "Source/Listener died, subscribing again")
        trySubscribeDelayed()
        context become idle
      case Done =>
        log.info("Source finished, subscribing again")
        trySubscribeDelayed()
        context become idle
    }
  }

  private def subscribe(): Unit = {
    log.info(s"Subscribing to ${messageLike.streamName}")

    val sink = Sink.foreach[M] { msg =>
      log.info(s"Processed ${messageLike.streamName} - ${messageLike.id(msg)}")
    }

    source.runWith(sink).pipeTo(self)

    context become subscribed
  }

  private def idle: Receive = {
    case Subscribe =>
      Try(subscribe()).failed.foreach { ex =>
        log.error(ex, "Could not subscribe, trying again")
        trySubscribeDelayed()
      }
  }

  override def receive: Receive = idle

  private def trySubscribeDelayed(delay: FiniteDuration = 5.seconds): Unit = {
    context.system.scheduler.scheduleOnce(delay, self, Subscribe)
  }
}

object MessageBusListenerActor {
  case object Subscribe

  def props[M](source: Source[M, NotUsed])(implicit ml: MessageLike[M]): Props
    = Props(new MessageBusListenerActor[M](source))
}
