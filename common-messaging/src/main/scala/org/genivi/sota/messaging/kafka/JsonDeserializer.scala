/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.messaging.kafka

import java.nio.ByteBuffer
import java.util

import cats.data.Xor
import io.circe.Decoder
import io.circe.jawn._
import org.apache.kafka.common.serialization.Deserializer

import scala.util.control.NoStackTrace

class JsonDeserializerException(msg: String) extends Exception(msg) with NoStackTrace

class JsonDeserializer[T](decoder: Decoder[T]) extends Deserializer[T] {


  override def deserialize(topic: String, data: Array[Byte]): T = {
    val buffer = ByteBuffer.wrap(data)

    val msgXor = parseByteBuffer(buffer).flatMap(_.as[T](decoder))

    msgXor match {
      case Xor.Right(v) => v
      case Xor.Left(ex) => throw new JsonDeserializerException(s"Could not parse msg from $topic: ${ex.getMessage}")
    }
  }

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()

  override def close(): Unit = ()
}
