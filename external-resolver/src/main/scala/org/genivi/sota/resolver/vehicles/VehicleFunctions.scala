/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.vehicles

import org.genivi.sota.resolver.packages.{Package, PackageFunctions}
import org.genivi.sota.resolver.common.Errors
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcBackend.Database


object VehicleFunctions {

  case object MissingVehicle extends Throwable

  def exists
    (vin: Vehicle.Vin)
    (implicit db: Database, ec: ExecutionContext): Future[Vehicle] =
    db.run(VehicleRepository.exists(vin))
      .flatMap(_
        .fold[Future[Vehicle]]
          (Future.failed(MissingVehicle))(Future.successful(_)))

  def installPackage
    (vin: Vehicle.Vin, pkgId: Package.Id)
    (implicit db: Database, ec: ExecutionContext): Future[Unit] =
    for {
      _ <- exists(vin)
      _ <- PackageFunctions.exists(pkgId)
      _ <- db.run(VehicleRepository.installPackage(vin, pkgId))
    } yield ()

  def uninstallPackage
    (vin: Vehicle.Vin, pkgId: Package.Id)
    (implicit db: Database, ec: ExecutionContext): Future[Unit] =
    for {
      _ <- exists(vin)
      _ <- PackageFunctions.exists(pkgId)
      _ <- db.run(VehicleRepository.uninstallPackage(vin, pkgId))
    } yield ()

  def packagesOnVinMap
    (implicit db: Database, ec: ExecutionContext)
      : Future[Map[Vehicle.Vin, Seq[Package.Id]]] =
    db.run(VehicleRepository.listInstalledPackages)
      .map(_
        .sortBy(_._1)
        .groupBy(_._1)
        .mapValues(_.map(_._2)))

  def packagesOnVin
    (vin: Vehicle.Vin)
    (implicit db: Database, ec: ExecutionContext): Future[Seq[Package.Id]] =
    for {
      _  <- exists(vin)
      ps <- packagesOnVinMap
              .map(_
                .get(vin)
                .toList
                .flatten)
    } yield ps

  def vinsThatHavePackageMap
    (implicit db: Database, ec: ExecutionContext)
      : Future[Map[Package.Id, Seq[Vehicle.Vin]]] =
    db.run(VehicleRepository.listInstalledPackages)
      .map(_
        .sortBy(_._2)
        .groupBy(_._2)
        .mapValues(_.map(_._1)))

  def vinsThatHavePackage
    (pkgId: Package.Id)
    (implicit db: Database, ec: ExecutionContext): Future[Seq[Vehicle.Vin]] =
    for {
      _  <- PackageFunctions.exists(pkgId)
      vs <- vinsThatHavePackageMap
              .map(_
                .get(pkgId)
                .toList
                .flatten)
    } yield vs

}
