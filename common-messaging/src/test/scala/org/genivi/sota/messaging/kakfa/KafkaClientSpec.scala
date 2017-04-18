/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.messaging.kakfa

import java.time.Instant

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import cats.data.Xor
import org.genivi.sota.data.{Namespace, Uuid}
import org.genivi.sota.messaging.Messages.DeviceSeen
import org.genivi.sota.messaging.kafka.KafkaClient
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, ShouldMatchers}

import scala.concurrent.duration._
import scala.concurrent.Future

class KafkaClientSpec extends TestKit(ActorSystem("KafkaClientSpec"))
  with FunSuiteLike
  with ShouldMatchers
  with BeforeAndAfterAll
  with ScalaFutures
  with PatienceConfiguration  {

  implicit val _ec = system.dispatcher
  implicit val _mat = ActorMaterializer()

  override implicit def patienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(500, Millis))

  val publisher = KafkaClient.publisher(system, system.settings.config) match {
    case Xor.Right(p) => p
    case Xor.Left(ex) => throw ex
  }

  test("can send an event to bus") {
    val testMsg = DeviceSeen(Namespace("ns"), Uuid.generate(), Instant.now)
    val f = publisher.publish(testMsg).map(_ => 0)
    f.futureValue shouldBe 0
  }

  test("can send-receive events from bus") {
    val testMsg = DeviceSeen(Namespace("ns"), Uuid.generate(), Instant.now)

    val source = KafkaClient.source[DeviceSeen](system, system.settings.config) match {
      case Xor.Right(s) => s
      case Xor.Left(ex) => throw ex
    }

    val msgFuture = source.groupedWithin(10, 5.seconds).runWith(Sink.head)

    for {
      _ <- akka.pattern.after(3.seconds, system.scheduler)(Future.successful(()))
      _ <- publisher.publish(testMsg)
    } yield ()

    msgFuture.futureValue should contain(testMsg)
  }

  test("can send-receive and commit events from bus") {
    val testMsg = DeviceSeen(Namespace("ns"), Uuid.generate(), Instant.now)

    val source = KafkaClient.committableSource[DeviceSeen](system.settings.config) match {
      case Xor.Right(s) => s
      case Xor.Left(ex) => throw ex
    }

    val msgFuture = source
      .groupedWithin(10, 5.seconds)
      .map(g => (g.foldLeft(CommittableOffsetBatch.empty) { (batch, elem) => batch.updated(elem.committableOffset) }, g))
      .mapAsync(1) { case (offsets, elements) => offsets.commitScaladsl().map(_ => elements.map(_.record.value())) }
      .runWith(Sink.head)

    for {
      _ <- akka.pattern.after(3.seconds, system.scheduler)(Future.successful(()))
      _ <- publisher.publish(testMsg)
    } yield ()

    msgFuture.futureValue should contain(testMsg)
  }

}
