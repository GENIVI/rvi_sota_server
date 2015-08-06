/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.rest

case class ErrorCode( code: String ) extends AnyVal

object ErrorCode {
  import spray.json.{JsString, JsValue, RootJsonFormat, deserializationError}

  implicit object Format extends RootJsonFormat[ErrorCode] {
    override def write( x : ErrorCode ) : JsValue = JsString( x.code )

    override def read(value : JsValue ) : ErrorCode = value match {
      case JsString(x) => ErrorCode(x)
      case _ => deserializationError("Error code expected")
    }
  }

}

object ErrorCodes {
  val InvalidEntity = new ErrorCode("invalid_entity")
  val DuplicateEntry = new ErrorCode("duplicate_entry")
}

case class ErrorRepresentation( code: ErrorCode, description: String )

object ErrorRepresentation {
  import spray.json.DefaultJsonProtocol._

  implicit val format = jsonFormat2(ErrorRepresentation.apply)
}
