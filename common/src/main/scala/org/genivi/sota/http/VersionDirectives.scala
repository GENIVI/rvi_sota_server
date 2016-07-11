/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.http

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directive0, Directives}

object VersionDirectives {
  def versionHeaders(version: String): Directive0 = {
    val header = RawHeader("x-ats-version", version)
    Directives.respondWithHeader(header)
  }
}
