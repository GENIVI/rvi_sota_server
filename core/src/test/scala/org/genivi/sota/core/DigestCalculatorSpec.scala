/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuiteLike, ShouldMatchers}

class DigestCalculatorSpec extends TestKit(ActorSystem("DigestCalculatorTest"))
  with FunSuiteLike
  with ShouldMatchers
  with ScalaFutures
  with DefaultPatience {

  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  test("calculates digest for a string") {
    val strings = List(ByteString("Hello"))
    val source = Source(strings)

    val digest = source.runWith(DigestCalculator("SHA-1")).futureValue
    digest shouldBe "f7ff9e8b7bb2e09b70935a5d785e0cc5d9d0abf0"
  }

  test("calculates digest for a file") {
    val tempFile = File.createTempFile("testfile", ".txt")

    val ioResult = Source.single(ByteString("Some text"))
      .runWith(FileIO.toPath(tempFile.toPath))

    whenReady(ioResult) { _ =>
        val digest = FileIO.fromPath(tempFile.toPath)
        .runWith(DigestCalculator("SHA-1")).futureValue

      digest shouldBe "02d92c580d4ede6c80a878bdd9f3142d8f757be8"
    }
  }
}
