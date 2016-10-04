/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.daemon

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.stream.scaladsl.{Sink, Source}
import cats.data.Xor
import com.typesafe.config.Config
import org.genivi.sota.messaging.Messages.{MessageLike, PackageCreated}
import org.genivi.sota.messaging.daemon.MessageBusListenerActor
import org.genivi.sota.messaging.daemon.MessageBusListenerActor.Subscribe
import org.genivi.sota.messaging.kafka.KafkaClient
import org.genivi.sota.resolver.db.Package.Metadata
import org.genivi.sota.resolver.db.PackageRepository
import org.slf4j.LoggerFactory
import slick.driver.MySQLDriver.api._


object PackageCreatedListener {
  def buildSource(db: Database,
                  fromSource: Source[CommittableMessage[Array[Byte], PackageCreated], NotUsed])
                 (implicit system: ActorSystem, ml: MessageLike[PackageCreated]): Source[PackageCreated, NotUsed] = {
    implicit val ec = system.dispatcher

    fromSource
      .mapAsync(3) { commitableMsg =>
        val msg = commitableMsg.record.value()
        val dbIO = PackageRepository.add(msg.packageId, Metadata(msg.namespace, msg.description, msg.vendor))
        db.run(dbIO).map(_ => commitableMsg)
      }
      .mapAsync(1) { commitableMsg =>
        commitableMsg.committableOffset.commitScaladsl().map(_ => commitableMsg.record.value())
      }
  }

  private def kafkaSource(config: Config)
                         (implicit system: ActorSystem, ml: MessageLike[PackageCreated])
  : Source[CommittableMessage[Array[Byte], PackageCreated], NotUsed] =
    KafkaClient.commitableSource[PackageCreated](config)(ml, system) match {
      case Xor.Right(s) => s
      case Xor.Left(err) => throw err
    }

  def props(db: Database, config: Config)(implicit system: ActorSystem, ml: MessageLike[PackageCreated]): Props = {
    val source = buildSource(db, kafkaSource(config))
    MessageBusListenerActor.props[PackageCreated](source)(ml)
  }
}


object MessageBusListener extends App {
  implicit val system = ActorSystem("sota-resolver-bus-listener")
  implicit val exec = system.dispatcher
  implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val db = Database.forConfig("database")

  val ref = system.actorOf(PackageCreatedListener.props(db, system.settings.config))
  ref ! Subscribe
}
