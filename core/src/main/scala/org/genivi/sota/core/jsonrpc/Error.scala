/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.jsonrpc

import io.circe.Json

final case class JsonRpcError(code: Int, message: String, data: Option[Json])

object JsonRpcError {

  def apply( code: Int, message: String ) : JsonRpcError = JsonRpcError( code, message, None )

}

final case class ErrorResponse(error: JsonRpcError, id: Option[Int])
