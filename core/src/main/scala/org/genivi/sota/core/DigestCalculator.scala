/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.security.MessageDigest

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import org.apache.commons.codec.binary.Hex
import org.genivi.sota.core.DigestCalculator.DigestResult

object DigestCalculator {
  type DigestResult = String

  def apply(algorithm: String = "SHA-1"): DigestCalculator = new DigestCalculator(algorithm)
}

class DigestCalculator(algorithm: String) extends GraphStage[FlowShape[ByteString, String]] {
  val in = Inlet[ByteString]("Digest.in")
  val out = Outlet[DigestResult]("Digest.out")

  override def shape: FlowShape[ByteString, DigestResult] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {
      val digest = MessageDigest.getInstance(algorithm)

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val chunk = grab(in)
          digest.update(chunk.toArray)
          pull(in)
        }

        override def onUpstreamFinish(): Unit = {
          val hexDigest = Hex.encodeHexString(digest.digest())
          emit(out, hexDigest)
          super.onUpstreamFinish()
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
      })
    }
  }
}
