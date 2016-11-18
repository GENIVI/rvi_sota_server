/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.http

import akka.event.Logging
import akka.event.Logging.LogLevel
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Directive0, Directives}

object LogDirectives {
  import Directives._

  type MetricsBuilder = (HttpRequest, HttpResponse) => Map[String, String]

  lazy val envServiceName = sys.env.get("SERVICE_NAME")

  def logResponseMetrics(defaultServiceName: String,
                         extraMetrics: MetricsBuilder = (_, _) => Map.empty,
                         level: LogLevel = Logging.InfoLevel): Directive0 = {
    val serviceName = envServiceName.getOrElse(defaultServiceName)

    extractRequestContext flatMap { ctx =>
      val startAt = System.currentTimeMillis()
      mapResponse { resp =>
        val responseTime = System.currentTimeMillis() - startAt
        val allMetrics =
          defaultMetrics(ctx.request, resp, responseTime) ++
            extraMetrics(ctx.request, resp) ++ Map("service_name" -> serviceName)

        ctx.log.log(level, formatResponseLog(allMetrics))

        resp
      }
    }
  }

  private def defaultMetrics(request: HttpRequest, response: HttpResponse, serviceTime: Long):
  Map[String, String] = {
    Map(
      "method" -> request.method.name,
      "path" -> request.uri.path.toString,
      "query" -> s"'${request.uri.rawQueryString.getOrElse("").toString}'",
      "stime" -> serviceTime.toString,
      "status" -> response.status.intValue.toString
    )
  }

  private def formatResponseLog(metrics: Map[String, String]): String = {
    metrics.toList.map { case (m, v) => s"$m=$v"}.mkString(" ")
  }
}
