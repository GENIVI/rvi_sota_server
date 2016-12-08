/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.monitoring

import java.util.concurrent.TimeUnit

import com.codahale.metrics.{MetricRegistry, Slf4jReporter}
import org.genivi.sota.db.DatabaseConfig
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

object MetricsSupport {
  lazy val metrics = new MetricRegistry()
}

trait MetricsSupport {
  lazy val metrics = MetricsSupport.metrics

  private lazy val reporter = Slf4jReporter.forRegistry(metrics)
    .convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .build()

  reporter.start(1, TimeUnit.MINUTES)
}

trait DatabaseMetrics {
  self: MetricsSupport with DatabaseConfig =>

  db.source.asInstanceOf[HikariCPJdbcDataSource].ds.setMetricRegistry(metrics)
}
