/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.security.MessageDigest

import akka.stream.scaladsl.Sink
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import org.apache.commons.codec.binary.Hex
import org.genivi.sota.core.DigestCalculator.DigestResult

import scala.concurrent.{ExecutionContext, Future}

object DigestCalculator {
  type DigestResult = String

  def apply(algorithm: String = "SHA-1")(implicit ec: ExecutionContext): Sink[ByteString, Future[DigestResult]] = {
    val digest = MessageDigest.getInstance(algorithm)

    Sink.fold(digest) { (d, b: ByteString) =>
      d.update(b.toArray)
      d
    } mapMaterializedValue(_.map(dd => Hex.encodeHexString(dd.digest())))
  }
}
