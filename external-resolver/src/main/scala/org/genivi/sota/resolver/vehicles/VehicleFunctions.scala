/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.vehicles

import org.genivi.sota.resolver.packages.PackageRepository
import org.genivi.sota.resolver.packages.{Package, PackageFunctions}
import org.genivi.sota.resolver.common.Errors
import scala.concurrent.{ExecutionContext, Future}
import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database


object VehicleFunctions {

  def updateInstalledPackages(vin: Vehicle.Vin, packages: Set[Package.Id] )
                             (implicit db: Database, ec: ExecutionContext): Future[Unit] = {
    def checkPackagesAvailable( ids: Set[Package.Id] ) : DBIO[Unit] = {
      PackageRepository.load(ids).map( ids -- _.map(_.id) ).flatMap { xs =>
        if( xs.isEmpty ) DBIO.successful(()) else DBIO.failed( Errors.MissingPackageException )
      }
    }

    val action : DBIO[Unit] = for {
      vehicle           <- VehicleRepository.exists(vin)
      installedPackages <- VehicleRepository.installedOn(vin)
      newPackages       =  packages -- installedPackages
      deletedPackages   =  installedPackages -- packages
      _                 <- checkPackagesAvailable(newPackages)
      _                 <- VehicleRepository.updateInstalledPackages(vehicle, newPackages, deletedPackages)
    } yield ()
    db.run( action )
  }

}
