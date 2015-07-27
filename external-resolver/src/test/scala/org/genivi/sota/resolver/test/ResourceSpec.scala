/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.zaxxer.hikari.util.DriverDataSource
import org.flywaydb.core.Flyway
import org.scalatest.BeforeAndAfterAll
import org.scalatest.prop.PropertyChecks
import org.scalatest.{WordSpec, PropSpec, Matchers}

object Migrations {
  def run(url: String, user: String, password: String) = {
    val flyway = new Flyway

    flyway.setDataSource(url, user, password)
    flyway.setLocations("classpath:db.migration")
    flyway.clean()
    flyway.migrate()
  }
}

trait ResourceSpec extends Matchers with ScalatestRouteTest { self: org.scalatest.Suite =>

  import akka.http.scaladsl.model.Uri
  import akka.http.scaladsl.model.Uri.Path
  import org.genivi.sota.resolver.db.{Vins, Packages}
  import org.genivi.sota.resolver.types.{Vin, Package}
  import slick.jdbc.JdbcBackend.Database

  // Paths
  def resourceUri(pathSuffix: String): Uri = {
    Uri.Empty.withPath(BasePath / pathSuffix)
  }

  val BasePath     = Path("/api") / "v1"
  val VinsUri      = resourceUri("vins")
  val PackagesUri  = resourceUri("packages")

  // Database
  val db = Database.forConfig("test-database")

  override def beforeAll() = {
    val conf = system.settings.config.getConfig("test-database")
    Migrations.run( conf.getString("url"), conf.getString("properties.user"), conf.getString("properties.password") )
  }

  // Route
  lazy val route = new org.genivi.sota.resolver.Route(db).route

  override def afterAll() {
    system.shutdown()
    db.close()
  }
}

trait ResourceWordSpec extends WordSpec with ResourceSpec
trait ResourcePropSpec extends PropSpec with ResourceSpec with PropertyChecks
