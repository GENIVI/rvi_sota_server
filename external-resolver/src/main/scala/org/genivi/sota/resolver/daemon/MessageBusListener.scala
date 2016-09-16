/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.daemon

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, ActorSystem, PoisonPill, Props}
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnError, OnNext}
import akka.stream.actor.{ActorSubscriber, RequestStrategy, WatermarkRequestStrategy}
import org.genivi.sota.data.PackageId._
import org.genivi.sota.messaging.Messages.PackageCreated
import org.genivi.sota.resolver.db.PackageRepository
import org.slf4j.LoggerFactory
import slick.driver.MySQLDriver.api._
import cats.syntax.show.toShowOps
import akka.pattern.pipe
import org.genivi.sota.data.PackageId
import org.genivi.sota.messaging.daemon.MessageBusListenerActor
import org.genivi.sota.messaging.daemon.MessageBusListenerActor.Subscribe
import org.genivi.sota.messaging.Messages._
import org.genivi.sota.resolver.db.{Package, PackageRepository}

class PackageCreatedListener(db: Database) extends ActorSubscriber with ActorLogging {

  override protected def requestStrategy: RequestStrategy = WatermarkRequestStrategy(1024)

  implicit val ec = context.dispatcher

  override def receive: Receive = {
    case Failure(ex) =>
      log.error(ex, "Could not save package")

    case pId: PackageId =>
      log.info(s"Saved package from bus (id: ${pId.show})")

    case OnNext(e: PackageCreated) =>
      val p = Package(e.namespace, e.packageId, e.description, e.vendor)
      val dbIO = PackageRepository.add(p)
      db.run(dbIO).map(_ => p.id).pipeTo(self)

    case OnComplete =>
      log.info("PackageCreated Upstream completed")
      self ! PoisonPill

    case OnError(ex) =>
      log.info(s"Error from upstream: ${ex.getMessage}")
      throw ex
  }
}

object ResolverMessageBusListenerActor {
  def props(db : Database): Props =
    MessageBusListenerActor.props[PackageCreated](Props(classOf[PackageCreatedListener], db))
}


object MessageBusListener extends App {
  implicit val system = ActorSystem("sota-resolver-bus-listener")
  implicit val exec = system.dispatcher
  implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val db = Database.forConfig("database")

  val ref = system.actorOf(ResolverMessageBusListenerActor.props(db))
  ref ! Subscribe
}
