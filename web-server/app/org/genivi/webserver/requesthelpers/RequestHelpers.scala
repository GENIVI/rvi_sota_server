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

  def chooseResponse(leftCode : Int, rightCode : Int) : ResponseToTake = {
    (leftCode, rightCode) match {
      case (_, 404) => LeftResponse()
      case (404, _) => RightResponse()
      case (204, 204) => LeftResponse() // No Content
      case (200, 200) => LeftResponse() // Temporary, while packages are in core+resolver
      case _ => ErrorResponse("Got " + leftCode + " and " + rightCode + " http responses")
    }
  }
}
