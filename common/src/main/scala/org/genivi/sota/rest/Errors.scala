/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.rest

import cats.data.Xor
import io.circe.{Encoder, Decoder, Json}
import Json.{obj, string}

object ErrorCodes {
  val InvalidEntity = new ErrorCode("invalid_entity")
  val DuplicateEntry = new ErrorCode("duplicate_entry")
}

case class ErrorRepresentation( code: ErrorCode, description: String )

object ErrorRepresentation {
  import io.circe.generic.semiauto._
  implicit val encoderInstance = deriveFor[ErrorRepresentation].encoder
  implicit val decoderInstance = deriveFor[ErrorRepresentation].decoder

}

case class ErrorCode(code: String) extends AnyVal

object ErrorCode {
  implicit val encoderInstance : Encoder[ErrorCode] = Encoder[String].contramap( _.code )
  implicit val decoderInstance : Decoder[ErrorCode] = Decoder[String].map( ErrorCode.apply )
}

trait SotaError extends Throwable

object SotaError {

  implicit def errorEncoder[T <: SotaError]: Encoder[T] = {
    def classNameToCode( cl: Class[_] ) = {
      val sn = cl.getSimpleName()
      if( sn.endsWith("$")) sn.substring(0, sn.length() -1 ) else sn
    }
    Encoder.instance { t =>
      obj(
        "code" -> string( classNameToCode(t.getClass)),
        "description" -> string(t.getMessage)
      )
    }
  }

  implicit def xorEncoder[A, B](implicit ea: Encoder[A], eb: Encoder[B]): Encoder[Xor[A, B]] =
    Encoder.instance(_.fold(ea.apply(_), eb.apply(_)))

  def errorCode(json: Json): Decoder.Result[String] =
    json.hcursor.downField("code").as[String]

}
