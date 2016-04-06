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


/**
 * Generic trait for REST specs
 * Includes helpers for Packages, Components, Filters, PackageFilters and
 * Resolver
 */
trait ResourceSpec extends
         VehicleRequests
    with PackageRequests
    with FirmwareRequests
    with ComponentRequests
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
    system.terminate()
    db.close()
  }

  // Route
  lazy implicit val route: Route = new Routing().route

}

/**
 * Generic trait for REST Word Specs
 * Includes helpers for Packages, Components, Filters, PackageFilters and
 * Resolver
 */
trait ResourceWordSpec extends WordSpec with ResourceSpec

/**
 * Generic trait for REST Property specs
 * Includes helpers for Packages, Components, Filters, PackageFilters and
 * Resolver
 */
trait ResourcePropSpec extends PropSpec with ResourceSpec with PropertyChecks
