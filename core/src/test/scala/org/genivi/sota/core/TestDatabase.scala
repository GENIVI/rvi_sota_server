/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.genivi.sota.core.common.NamespaceDirective
import org.genivi.sota.core.data.{Package, UpdateRequest, UpdateSpec}
import org.genivi.sota.core.db.{Packages, UpdateRequests, UpdateSpecs, Vehicles}
import org.genivi.sota.data.Namespace.Namespace
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import org.genivi.sota.data.{Vehicle, VehicleGenerators}

import scala.concurrent.Future
import org.genivi.sota.db.SlickExtensions
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.concurrent.{AbstractPatienceConfiguration, PatienceConfiguration}
import org.scalatest.time.{Millis, Seconds, Span}

import scala.util.Try

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

trait DefaultDBPatience {
  self: PatienceConfiguration =>

  override implicit def patienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
}

trait DatabaseSpec extends BeforeAndAfterAll {
  self: Suite =>

  import eu.timepit.refined.auto._

  private val databaseId = "test-database"

  lazy val db = Database.forConfig(databaseId)

  private lazy val config = ConfigFactory.load()

  val defaultNamespace: Namespace =
    NamespaceDirective.configNamespace(config).getOrElse("default-test-ns")

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

  def createUpdateSpecFor(vehicle: Vehicle, transformFn: UpdateRequest => UpdateRequest = identity)(implicit ec: ExecutionContext): DBIO[(Package, UpdateSpec)] = {
    val (packageModel, updateSpec) = genUpdateSpecFor(vehicle).sample.get

    val dbIO = DBIO.seq(
      Packages.create(packageModel),
      UpdateRequests.persist(transformFn(updateSpec.request)),
      UpdateSpecs.persist(updateSpec)
    )

    dbIO.map(_ => (packageModel, updateSpec))
  }

  def createUpdateSpecAction()(implicit ec: ExecutionContext): DBIO[(Package, Vehicle, UpdateSpec)] = {
    val vehicle = VehicleGenerators.genVehicle.sample.get

    for {
      _ <- Vehicles.create(vehicle)
      (packageModel, updateSpec) <- createUpdateSpecFor(vehicle)
    } yield (packageModel, vehicle, updateSpec)
  }

  def createUpdateSpec()(implicit ec: ExecutionContext): Future[(Package, Vehicle, UpdateSpec)] = {
    db.run(createUpdateSpecAction())
  }
}

trait VehicleDatabaseSpec {
  self: DatabaseSpec =>

  def createVehicle()(implicit ec: ExecutionContext): Future[Vehicle] = {
    val vehicle = VehicleGenerators.genVehicle.sample.get
    db.run(Vehicles.create(vehicle))
  }
}
