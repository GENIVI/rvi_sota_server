/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.webserver.requesthelpers

abstract class ResponseToTake
case class LeftResponse() extends ResponseToTake
case class RightResponse() extends ResponseToTake
case class ErrorResponse(msg : String) extends ResponseToTake

object RequestHelper {
  val Ok = 200
  val NoContent = 204
  val NotFound = 404

  def chooseResponse(leftCode : Int, rightCode : Int) : ResponseToTake = {
    (leftCode, rightCode) match {
      case (_, NotFound) => LeftResponse()
      case (NotFound, _) => RightResponse()
      case (NoContent, NoContent) => LeftResponse() // No Content
      case (Ok, Ok) => LeftResponse() // Temporary, while packages are in core+resolver
      case _ => ErrorResponse("Got " + leftCode + " and " + rightCode + " http responses")
    }
  }
}
