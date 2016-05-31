/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core

import com.typesafe.config.{Config, ConfigFactory}
import org.genivi.sota.core.data.{Package, UpdateRequest, UpdateSpec}
import org.genivi.sota.core.db.{Packages, UpdateRequests, UpdateSpecs, Vehicles}
import org.genivi.sota.data.Namespace.Namespace

import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import org.genivi.sota.data.{Vehicle, VehicleGenerators}
import org.genivi.sota.datatype.NamespaceDirective

import scala.concurrent.Future


object NamespaceSpec {
  import eu.timepit.refined.auto._
  import eu.timepit.refined.string._
  import org.genivi.sota.data.Namespace._

  lazy val defaultNamespace: Namespace = {
    val config = ConfigFactory.load()
    NamespaceDirective.configNamespace(config).getOrElse("default-test-ns")
  }
}


trait UpdateResourcesDatabaseSpec {
  self: DatabaseSpec =>

  import Generators._

  def createUpdateSpecFor(vehicle: Vehicle, installPos: Int = 0)
                         (implicit ec: ExecutionContext): DBIO[(Package, UpdateSpec)] = {
    val (packageModel, updateSpec0) = genUpdateSpecFor(vehicle).sample.get
    val updateSpec = updateSpec0.copy(installPos = installPos)

    val dbIO = DBIO.seq(
      Packages.create(packageModel),
      UpdateRequests.persist(updateSpec.request),
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
