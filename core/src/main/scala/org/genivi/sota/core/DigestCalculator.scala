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

object DigestCalculator {
  def apply(algorithm: String): DigestCalculator = new DigestCalculator(algorithm)
}

class DigestCalculator(algorithm: String) extends GraphStage[FlowShape[ByteString, String]] {
  val in = Inlet[ByteString]("Digest.in")
  val out = Outlet[String]("Digest.out")

  override def shape: FlowShape[ByteString, String] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {
      val digest = MessageDigest.getInstance(algorithm)

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val chunk = grab(in)
          digest.update(chunk.toArray)
          pull(in)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          if(isClosed(in)) {
            val h = Hex.encodeHexString(digest.digest())
            push(out, h)
            completeStage()
          } else {
            pull(in)
          }
        }
      })
    }
  }
}
