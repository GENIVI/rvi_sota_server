/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core

import akka.http.scaladsl.util.FastFuture
import com.typesafe.config.{Config, ConfigFactory}
import org.genivi.sota.core.common.NamespaceDirective
import org.genivi.sota.core.data.{Package, UpdateRequest, UpdateSpec}
import org.genivi.sota.core.db.{Packages, UpdateRequests, UpdateSpecs}
import org.genivi.sota.data.Namespace.Namespace
import org.genivi.sota.data.{Device, DeviceT, Vehicle, VehicleGenerators}
import org.genivi.sota.device_registry.IDeviceRegistry
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.driver.MySQLDriver.api._


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

  def createUpdateSpecFor(vehicle: Vehicle, transformFn: UpdateRequest => UpdateRequest = identity)
                         (implicit ec: ExecutionContext): DBIO[(Package, UpdateSpec)] = {
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
      (packageModel, updateSpec) <- createUpdateSpecFor(vehicle)
    } yield (packageModel, vehicle, updateSpec)
  }

  def createUpdateSpec()(implicit ec: ExecutionContext): Future[(Package, Vehicle, UpdateSpec)] = {
    db.run(createUpdateSpecAction())
  }
}

trait VehicleDatabaseSpec {
  self: DatabaseSpec =>

  import Device._

  def createVehicle(deviceRegistry: IDeviceRegistry)(implicit ec: ExecutionContext): Future[Vehicle] = {
    val vehicle = VehicleGenerators.genVehicle.sample.get
    deviceRegistry.createDevice(DeviceT(Some(DeviceId(vehicle.vin.get)), DeviceType.Vehicle))
    FastFuture.successful(vehicle)
  }
}
