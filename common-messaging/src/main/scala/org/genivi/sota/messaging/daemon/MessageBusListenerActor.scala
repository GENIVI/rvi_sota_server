package org.genivi.sota.messaging.daemon

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import cats.data.Xor
import com.typesafe.config.ConfigException
import org.genivi.sota.messaging.MessageBus
import org.genivi.sota.messaging.Messages.{Message, MessageLike}
import org.genivi.sota.messaging.daemon.MessageBusListenerActor.Subscribe
import scala.concurrent.duration._
import scala.util.Try

class MessageBusListenerActor[Msg <: Message](props: Props,
                                              mlArg: MessageLike[Msg]) extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher
  implicit val ml = mlArg

  override def postRestart(reason: Throwable): Unit = trySubscribeDelayed()

  private def subscribed(listener: ActorRef): Receive = {
    context watch listener

    {
      case Terminated(_) =>
        log.error("Source/Listener died, subscribing again")
        trySubscribeDelayed()
        context become idle
    }
  }


  private def subscribe(): Unit = {
    log.info(s"Subscribing to ${ml.streamName}")
    subscribeFn() match {
      case Xor.Right(source) =>
        val ref = source.runWith(Sink.actorSubscriber(props))
        context become subscribed(ref)
      case Xor.Left(err) =>
        throw err
    }
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

  protected def subscribeFn(): ConfigException Xor Source[Msg, NotUsed] =
    MessageBus.subscribe[Msg](context.system, context.system.settings.config)
}

object MessageBusListenerActor {
  case object Subscribe

  def props[Msg <: Message](props: Props)(implicit ml: MessageLike[Msg]): Props
    = Props(classOf[MessageBusListenerActor[Msg]],props,ml)
}
