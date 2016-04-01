/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.genivi.sota.core.data.{Package, UpdateSpec}
import org.genivi.sota.core.db.{Packages, UpdateRequests, UpdateSpecs, Vehicles}
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import org.genivi.sota.data.{Vehicle, VehicleGenerators}

import scala.concurrent.Future
import org.genivi.sota.db.SlickExtensions

/*
 * Helper object to configure test database for specs
 */
object TestDatabase {

  def resetDatabase( databaseName: String ) = {
    val dbConfig = ConfigFactory.load().getConfig(databaseName)
    val url = dbConfig.getString("url")
    val user = dbConfig.getConfig("properties").getString("user")
    val password = dbConfig.getConfig("properties").getString("password")

    val flyway = new Flyway
    flyway.setDataSource(url, user, password)
    flyway.setLocations("classpath:db.migration")
    flyway.clean()
    flyway.migrate()
  }
}

trait DatabaseSpec extends BeforeAndAfterAll {
  self: Suite =>

  private val databaseId = "test-database"

  lazy val db = Database.forConfig(databaseId)

  override def beforeAll() {
    TestDatabase.resetDatabase(databaseId)
    super.beforeAll()
  }

  override def afterAll() {
    db.close()
    super.afterAll()
  }
}

trait UpdateResourcesDatabaseSpec {
  self: DatabaseSpec =>

  import Generators._
  // import SlickExtensions._

  def createUpdateSpec()(implicit ec: ExecutionContext): Future[(Package, Vehicle, UpdateSpec)] = {
    val (packageModel, vehicle, updateSpec) = updateSpecGen.sample.get

    val dbIO = DBIO.seq(
      Packages.create(packageModel),
      Vehicles.create(vehicle),
      UpdateRequests.persist(updateSpec.request),
      UpdateSpecs.persist(updateSpec)
    )

    db.run(dbIO).map(_ => (packageModel, vehicle, updateSpec))
  }
}

trait VehicleDatabaseSpec {
  self: DatabaseSpec =>

  def createVehicle()(implicit ec: ExecutionContext): Future[Vehicle.Vin] = {
    val vehicle = VehicleGenerators.genVehicle.sample.get
    db.run(Vehicles.create(vehicle))
  }
}