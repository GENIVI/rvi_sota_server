/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.flywaydb.core.Flyway
import org.genivi.sota.resolver.Routing
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Suite, WordSpec, PropSpec, Matchers}
import slick.jdbc.JdbcBackend.Database


trait ResourceSpec extends
         VehicleRequests
    with PackageRequests
    with FilterRequests
    with PackageFilterRequests
    with ResolveRequests
    with ScalatestRouteTest
    with BeforeAndAfterAll { self: Suite =>

  // Database
  val name        = "test-database"
  implicit val db = Database.forConfig(name)

  override def beforeAll() = {
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

  override def afterAll() = {
    system.shutdown()
    db.close()
  }

  // Route
  lazy implicit val route: Route = new Routing().route

}

trait ResourceWordSpec extends WordSpec with ResourceSpec
trait ResourcePropSpec extends PropSpec with ResourceSpec with PropertyChecks
