/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.rest

import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import cats.data.Xor
import io.circe._
import Json.{obj, string}


case class ErrorCode( code: String ) extends AnyVal

case class ErrorRepresentation( code: ErrorCode, description: String )

object ErrorRepresentation {

  implicit val errorRepresentationEncoder: Encoder[ErrorRepresentation] =
    Encoder.instance { er =>
      obj( ("code",        string(er.code.code))
         , ("description", string(er.description))
         )
    }

  implicit val errorRepresentationDecoder: Decoder[ErrorRepresentation] =
    Decoder.instance { c =>
      c.focus.asObject match {
        case None      => Xor.left(DecodingFailure("ErrorRepresentation", c.history))
        case Some(obj) => (obj.toMap.get("code")       .flatMap(_.asString),
                           obj.toMap.get("description").flatMap(_.asString)) match {
          case (Some(code), Some(desc)) => Xor.right(ErrorRepresentation(ErrorCode(code), desc))
          case _                        => Xor.left(DecodingFailure("ErrorRepresentation", c.history))
        }
      }
    }

}

object ErrorCodes {
  val InvalidEntity  = new ErrorCode("invalid_entity")
  val DuplicateEntry = new ErrorCode("duplicate_entry")
}
