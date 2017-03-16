/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.monitoring

import java.util.concurrent.TimeUnit

import com.codahale.metrics.jvm.{GarbageCollectorMetricSet, MemoryUsageGaugeSet}

import scala.collection.JavaConverters._
import com.codahale.metrics._
import org.genivi.sota.db.DatabaseConfig
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

object MetricsSupport {
  lazy val metricRegistry = new MetricRegistry()

  val JvmFilter = new MetricFilter {
    override def matches(name: String, metric: Metric): Boolean = name.startsWith("jvm")
  }

  val DbFilter = new MetricFilter {
    override def matches(name: String, metric: Metric): Boolean = name.startsWith("database")
  }
}

trait MetricsSupport {
  lazy val metricRegistry = MetricsSupport.metricRegistry

  private lazy val reporter = Slf4jReporter.forRegistry(metricRegistry)
    .convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .filter(MetricsSupport.DbFilter)
    .build()

  private def registerAll(registry: MetricRegistry, prefix: String, metricSet: MetricSet): Unit = {
    metricSet.getMetrics.asScala.foreach {
      case (metricPrefix, set: MetricSet) =>
        registerAll(registry, prefix + "." + metricPrefix, set)
      case (metricPrefix, metric) =>
        registry.register(prefix + "." + metricPrefix, metric)
    }
  }

  registerAll(metricRegistry, "jvm.gc", new GarbageCollectorMetricSet())
  registerAll(metricRegistry, "jvm.memory", new MemoryUsageGaugeSet())

  reporter.start(1, TimeUnit.MINUTES)
}

trait DatabaseMetrics {
  self: MetricsSupport with DatabaseConfig =>

  db.source.asInstanceOf[HikariCPJdbcDataSource].ds.setMetricRegistry(metricRegistry)
}
