/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.data

import io.circe._
import io.circe.generic.semiauto._
import io.circe.generic.auto._
import org.genivi.sota.core.jsonrpc.{ErrorResponse, Request, JsonRpcError}
import org.genivi.sota.rest.ErrorRepresentation

trait CodecInstances {

  implicit val vinDecoder : Decoder[Vehicle.IdentificationNumber] = deriveFor[Vehicle.IdentificationNumber].decoder

  implicit val packageIdDecoder : Decoder[Package.Id] = deriveFor[Package.Id].decoder

  implicit val errorRepresentationDecoder : Decoder[ErrorRepresentation] = deriveFor[ErrorRepresentation].decoder

  implicit val errorRepresentationEncoder : Encoder[ErrorRepresentation] = deriveFor[ErrorRepresentation].encoder

  implicit val errorResponseEncoder : Encoder[ErrorResponse] = deriveFor[ErrorResponse].encoder

  implicit val errorResponseDecoder : Decoder[ErrorResponse] = deriveFor[ErrorResponse].decoder

  implicit val jsonRpcRequestDecoder : Decoder[Request] = deriveFor[Request].decoder
}

object CodecInstances extends CodecInstances
