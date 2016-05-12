/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.http

import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directive0, Directives}

object SotaDirectives {
  import Directives._

  def versionHeaders(version: String): Directive0 = {
    val header = RawHeader("x-ats-version", version)
    respondWithHeader(header)
  }

  private def formatResponseLog(service: String, request: HttpRequest,
                                response: HttpResponse, serviceTime: Long): String = {
    val metrics = Map(
      "method" -> request.method.name,
      "path" -> request.uri.path.toString,
      "stime" -> serviceTime.toString,
      "status" -> response.status.intValue.toString
    )

    metrics.toList.map { case (m, v) => s"$m=$v"}.mkString(" ")
  }

  def logResponseMetrics(service: String, level: LogLevel = Logging.InfoLevel) = {
    extractRequestContext flatMap { ctx =>
      val startAt = System.currentTimeMillis()
      mapResponse { resp =>
        val responseTime = System.currentTimeMillis() - startAt
        ctx.log.log(level, formatResponseLog(service, ctx.request, resp, responseTime))
        resp
      }
    }
  }
}
