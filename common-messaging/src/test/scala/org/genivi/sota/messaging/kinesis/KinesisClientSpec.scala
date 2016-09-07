/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.messaging.kinesis

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import cats.data.Xor
import org.genivi.sota.data.{Device, Uuid}
import org.genivi.sota.messaging.Messages.DeviceSeen
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, ShouldMatchers}

import scala.concurrent.duration._
import scala.concurrent.Future

class KinesisClientSpec extends TestKit(ActorSystem("KinesisClientSpec"))
  with FunSuiteLike
  with ShouldMatchers
  with BeforeAndAfterAll
  with ScalaFutures
  with PatienceConfiguration  {

  val testMsg = DeviceSeen(Uuid.generate(), Instant.now)

  implicit val _ec = system.dispatcher
  implicit val _mat = ActorMaterializer()

  override implicit def patienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(500, Millis))

  val publisher = KinesisClient.publisher(system, system.settings.config) match {
    case Xor.Right(p) => p
    case Xor.Left(ex) => throw ex
  }

  test("can send an event to bus") {
    val f = publisher.publish(testMsg).map(_ => 0)
    f.futureValue shouldBe 0
  }

  test("can receive events from bus") {
    val source = KinesisClient.source[DeviceSeen](system, system.settings.config) match {
      case Xor.Right(s) => s
      case Xor.Left(ex) => throw ex
    }

    val msgFuture = source.runWith(Sink.head)

    for {
      _ <- akka.pattern.after(5.seconds, system.scheduler)(Future.successful(()))
      _ <- publisher.publish(testMsg)
    } yield ()

    msgFuture.futureValue shouldBe testMsg
  }
}

