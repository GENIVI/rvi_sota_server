/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.daemon

import akka.NotUsed
import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnError, OnNext}
import akka.stream.actor.{ActorSubscriber, RequestStrategy, WatermarkRequestStrategy}
import akka.stream.scaladsl.{Sink, Source}
import org.genivi.sota.data.PackageId._
import org.genivi.sota.messaging.MessageBus
import org.genivi.sota.messaging.Messages.PackageCreated
import org.genivi.sota.resolver.packages.{Package, PackageRepository}
import org.slf4j.LoggerFactory
import slick.driver.MySQLDriver.api._
import cats.syntax.show.toShowOps
import akka.pattern.pipe
import cats.data.Xor
import com.typesafe.config.ConfigException
import org.genivi.sota.data.PackageId
import org.genivi.sota.resolver.daemon.MessageBusListenerActor.Subscribe

import scala.concurrent.duration._
import org.genivi.sota.messaging.Messages.PackageCreated._
import org.genivi.sota.messaging.Messages._
import scala.util.Try


class PackageCreatedListener(db: Database) extends ActorSubscriber with ActorLogging {

  override protected def requestStrategy: RequestStrategy = WatermarkRequestStrategy(1024)

  implicit val ec = context.dispatcher

  override def receive: Receive = {
    case Failure(ex) =>
      log.error(ex, "Could not save package")

    case pId: PackageId =>
      log.info(s"Saved package from bus (id: ${pId.show})")
      context.stop(self)

    case OnNext(e: PackageCreated) =>
      val p = Package(e.namespace, e.pid, e.description, e.vendor)
      val dbIO = PackageRepository.add(p)
      db.run(dbIO).map(_ => p.id).pipeTo(self)

    case OnComplete =>
      log.info("Upstream completed")
      context.stop(self)

    case OnError(ex) =>
      log.info(s"Error from upstream: ${ex.getMessage}")
      throw ex
  }
}

object MessageBusListenerActor {
  case object Subscribe

  def props(db: Database) = Props(classOf[MessageBusListenerActor], db)
}

class MessageBusListenerActor(db: Database) extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()
  implicit val ec = context.dispatcher

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
    log.info(s"Subscribing to ${classOf[PackageCreated].streamName}")
    subscribeFn() match {
      case Xor.Right(source) =>
        val ref = source.runWith(Sink.actorSubscriber(Props(classOf[PackageCreatedListener], db)))
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

  protected def subscribeFn(): ConfigException Xor Source[PackageCreated, NotUsed] =
    MessageBus.subscribe[PackageCreated](context.system, context.system.settings.config)
}


object MessageBusListener extends App {
  implicit val system = ActorSystem("sota-resolver-bus-listener")
  implicit val exec = system.dispatcher
  implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val db = Database.forConfig("database")

  val ref = system.actorOf(MessageBusListenerActor.props(db))
  ref ! Subscribe
}
