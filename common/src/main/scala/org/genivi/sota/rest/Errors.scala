/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.rest

import io.circe.{Encoder, Decoder}

/**
  * Errors are presented to the user of the core and resolver API as
  * JSON objects, this is done semi-automatically by the Circe library.
  *
  * @see {@link https://github.com/travisbrown/circe}
  */

object ErrorCodes {
  val InvalidEntity = ErrorCode("invalid_entity")
  val MissingEntity = ErrorCode("missing_entity")
  val ConflictingEntity = ErrorCode("conflicting_entity")
  val TooManyElements = ErrorCode("too_many_elements")
}

case class ErrorRepresentation( code: ErrorCode, description: String )

object ErrorRepresentation {
  import io.circe.generic.semiauto._
  implicit val encoderInstance = deriveEncoder[ErrorRepresentation]
  implicit val decoderInstance = deriveDecoder[ErrorRepresentation]

}

case class ErrorCode(code: String) extends AnyVal

object ErrorCode {
  implicit val encoderInstance : Encoder[ErrorCode] = Encoder[String].contramap( _.code )
  implicit val decoderInstance : Decoder[ErrorCode] = Decoder[String].map( ErrorCode.apply )
}
