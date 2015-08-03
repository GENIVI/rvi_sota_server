/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.webserver.requesthelpers

import play.api.http.Status

object RequestHelper {

  def isSuccessfulStatusCode (code : Int) : Boolean = {
    //checks if status code is in range 200-299
    code >= Status.OK && code < Status.MULTIPLE_CHOICES
  }

}
