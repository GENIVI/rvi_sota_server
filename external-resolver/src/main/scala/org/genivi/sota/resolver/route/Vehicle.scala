/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.route

import cats.data.Xor
import scala.concurrent.{ExecutionContext, Future}
import org.genivi.sota.resolver.types.{Vehicle, Package}
import org.genivi.sota.resolver.db.{Vehicles, Packages, InstalledPackages}
import slick.jdbc.JdbcBackend.Database


object VehicleRoute {

  case object MissingVehicle extends Throwable
  case object MissingPackage extends Throwable

  def exists
    (vin: Vehicle.Vin)
    (implicit db: Database, ec: ExecutionContext)
      : Future[Vehicle] =
    db.run(Vehicles.list)
      .flatMap(_
        .filter(_.vin == vin)
        .headOption
        .fold[Future[Vehicle]]
          (Future.failed(MissingVehicle))(Future.successful(_)))

  def existsPackage
    (pkgId: Package.Id)
    (implicit db: Database, ec: ExecutionContext)
      : Future[Package] =
    db.run(Packages.list)
      .flatMap(_
        .filter(_.id == pkgId)
        .headOption
        .fold[Future[Package]]
          (Future.failed(MissingPackage))(Future.successful(_)))

  def installPackage
    (vin: Vehicle.Vin, pkgId: Package.Id)
    (implicit db: Database, ec: ExecutionContext)
      : Future[Unit] =
    for {
      _ <- exists(vin)
      _ <- existsPackage(pkgId)
      _ <- db.run(InstalledPackages.add(vin, pkgId))
    } yield ()

  def uninstallPackage
    (vin: Vehicle.Vin, pkgId: Package.Id)
    (implicit db: Database, ec: ExecutionContext)
      : Future[Unit] =
    for {
      _ <- exists(vin)
      _ <- existsPackage(pkgId)
      _ <- db.run(InstalledPackages.remove(vin, pkgId))
    } yield ()

  def packagesOnVinMap
    (implicit db: Database, ec: ExecutionContext)
      : Future[Map[Vehicle.Vin, Seq[Package.Id]]] =
    db.run(InstalledPackages.list)
      .map(_
        .sortBy(_._1)
        .groupBy(_._1)
        .mapValues(_.map(_._2)))

  def packagesOnVin
    (vin: Vehicle.Vin)
    (implicit db: Database, ec: ExecutionContext)
      : Future[Seq[Package.Id]] =
    for {
      _  <- exists(vin)
      ps <- packagesOnVinMap
      .map(_
        .get(vin)
        .toList
        .flatten)
    } yield ps

}
