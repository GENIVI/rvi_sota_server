/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.resolve

import akka.stream.ActorMaterializer
import org.genivi.sota.resolver.db.PackageFilters
import org.genivi.sota.resolver.filters.FilterAST._
import org.genivi.sota.resolver.filters.{FilterAST, And, True}
import org.genivi.sota.resolver.packages.{Package, PackageFunctions}
import org.genivi.sota.resolver.vehicles.{Vehicle, VehicleFunctions, VehicleRepository}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.jdbc.JdbcBackend.Database


object ResolveFunctions {

  def makeFakeDependencyMap
    (pkgId: Package.Id, vs: Seq[Vehicle])
      : Map[Vehicle.Vin, List[Package.Id]] =
    vs.map(vehicle => Map(vehicle.vin -> List(pkgId)))
      .foldRight(Map[Vehicle.Vin, List[Package.Id]]())(_++_)

  def resolve
    (pkgId: Package.Id)
    (implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext)
      : Future[Map[Vehicle.Vin, Seq[Package.Id]]] =
    for {
      _       <- PackageFunctions.exists(pkgId)
      (p, fs) <- db.run(PackageFilters.listFiltersForPackage(pkgId))
      vs      <- db.run(VehicleRepository.list)
      ps : Seq[Seq[Package.Id]]
              <- Future.sequence(vs.map(v => VehicleFunctions.packagesOnVin(v.vin)))
      vps: Seq[Tuple2[Vehicle, Seq[Package.Id]]]
              =  vs.zip(ps)
    } yield makeFakeDependencyMap(pkgId,
              vps.filter(query(fs.map(_.expression).map(parseValidFilter).foldLeft[FilterAST](True)(And)))
                 .map(_._1))
}
