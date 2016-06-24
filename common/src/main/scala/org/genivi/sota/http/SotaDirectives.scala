/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.http

import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directive, Directive0, Directives}

object SotaDirectives {
  import Directives._

  type MetricsBuilder = (HttpRequest, HttpResponse) => Map[String, String]

  def versionHeaders(version: String): Directive0 = {
    val header = RawHeader("x-ats-version", version)
    respondWithHeader(header)
  }

  private def defaultMetrics(request: HttpRequest, response: HttpResponse, serviceTime: Long):
  Map[String, String] = {
    Map(
      "method" -> request.method.name,
      "path" -> request.uri.path.toString,
      "stime" -> serviceTime.toString,
      "status" -> response.status.intValue.toString
    )
  }

  private def formatResponseLog(metrics: Map[String, String]): String = {
    metrics.toList.map { case (m, v) => s"$m=$v"}.mkString(" ")
  }

  def logResponseMetrics(service: String,
                         extraMetrics: MetricsBuilder = (_, _) => Map.empty,
                         level: LogLevel = Logging.InfoLevel): Directive0 = {
    extractRequestContext flatMap { ctx =>
      val startAt = System.currentTimeMillis()
      mapResponse { resp =>
        val responseTime = System.currentTimeMillis() - startAt
        val allMetrics =
          defaultMetrics(ctx.request, resp, responseTime) ++
            extraMetrics(ctx.request, resp) ++ Map("service_name" -> service)

        ctx.log.log(level, formatResponseLog(allMetrics))

        resp
      }
    }
  }
}
