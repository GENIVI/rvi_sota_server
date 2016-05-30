/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.device_registry.test

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.flywaydb.core.Flyway
import org.genivi.sota.device_registry.Routing
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Suite, WordSpec, PropSpec, Matchers}
import slick.jdbc.JdbcBackend.Database
import org.scalatest.Matchers


trait ResourceSpec extends DeviceRequests
    with Matchers
    with ScalatestRouteTest
    with BeforeAndAfterAll { self: Suite =>

  // Database
  val name        = "test-database"
  implicit val db = Database.forConfig(name)

  override def beforeAll(): Unit = {
    val dbConfig = system.settings.config.getConfig(name)
    val url      = dbConfig.getString("url")
    val user     = dbConfig.getConfig("properties").getString("user")
    val passwd   = dbConfig.getConfig("properties").getString("password")

    val flyway = new Flyway
    flyway.setDataSource(url, user, passwd)
    flyway.setLocations("classpath:db.migration")
    flyway.clean()
    flyway.migrate()
  }

  override def afterAll(): Unit = {
    system.terminate()
    db.close()
  }

  // Route
  lazy implicit val route: Route = new Routing().route

}

trait ResourcePropSpec extends PropSpec with ResourceSpec with PropertyChecks
