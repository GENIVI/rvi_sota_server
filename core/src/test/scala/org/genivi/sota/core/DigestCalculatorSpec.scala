/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.io.File

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuiteLike, ShouldMatchers}

import scala.concurrent.Future

class DigestCalculatorSpec extends TestKit(ActorSystem("DigestCalculatorTest"))
  with FunSuiteLike
  with ShouldMatchers
  with ScalaFutures {

  implicit val mat = ActorMaterializer()

  test("calculates digest for a string") {
    val strings = List(ByteString("Hello"))
    val source = Source(strings)

    val probe = TestSink.probe[String]

    val subscriber = source.via(DigestCalculator()).runWith(probe)
    val digest = subscriber.requestNext()
    digest shouldBe "f7ff9e8b7bb2e09b70935a5d785e0cc5d9d0abf0"
  }

  test("calculates digest for a file") {
    val tempFile = File.createTempFile("testfile", ".txt")

    val ioResult = Source.single(ByteString("Some text"))
      .runWith(FileIO.toFile(tempFile))

    whenReady(ioResult) { _ =>
      val probe = TestSink.probe[String]
      val subscriber = FileIO.fromFile(tempFile)
        .via(DigestCalculator()).runWith(probe)

      subscriber.requestNext() shouldBe "02d92c580d4ede6c80a878bdd9f3142d8f757be8"
    }
  }
}
