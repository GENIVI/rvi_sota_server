/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.test

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.zaxxer.hikari.util.DriverDataSource
import org.scalatest.BeforeAndAfterAll
import org.scalatest.prop.PropertyChecks
import org.scalatest.{WordSpec, PropSpec, Matchers}

trait ResourceSpec extends Matchers with ScalatestRouteTest { self: org.scalatest.Suite =>

  import akka.http.scaladsl.model.Uri
  import akka.http.scaladsl.model.Uri.Path
  import org.genivi.sota.resolver.db.{Vehicles, Packages}
  import org.genivi.sota.resolver.types.{Vehicle$, Package}
  import slick.jdbc.JdbcBackend.Database

  // Paths

  def resourceUri(pathSuffixes: String*): Uri = {
    Uri.Empty.withPath(pathSuffixes.foldLeft(BasePath)((ih, p) => ih / p))
  }

  val BasePath     = Path("/api") / "v1"
  val VinsUri      = (vin: String) => resourceUri("vehicles", vin)
  val PackagesUri  = resourceUri("packages")
  val ResolveUri   = (i: Long) => resourceUri("resolve", i.toString)
  val FiltersUri   = resourceUri("filters")
  val ValidateUri  = (s: String) => resourceUri("validate", s)

  // Database
  val db = Database.forConfig("test-database")

  override def beforeAll() = {
    TestDatabase.reset
  }

  override def afterAll() {
    system.shutdown()
    db.close()
  }

  // Route
  lazy val route = new org.genivi.sota.resolver.Routing(db).route
}

trait ResourceWordSpec extends WordSpec with ResourceSpec
trait ResourcePropSpec extends PropSpec with ResourceSpec with PropertyChecks
