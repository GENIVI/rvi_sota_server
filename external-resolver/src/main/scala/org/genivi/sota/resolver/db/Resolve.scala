/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import cats.data.Xor
import org.genivi.sota.resolver.db.PackageFilters.listFiltersForPackage
import org.genivi.sota.resolver.db.Vehicles.vehicles
import org.genivi.sota.resolver.types.FilterParser.parseValidFilter
import org.genivi.sota.resolver.types.FilterQuery.query
import org.genivi.sota.resolver.types.{True, And}
import org.genivi.sota.resolver.types.{Vehicle, Package, FilterAST}
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


object Resolve {

/*
  def resolve
    (name: Package.Name, version: Package.Version)
    (implicit ec: ExecutionContext)
      : DBIO[Xor[PackageFilters.MissingPackageException, Map[Vehicle.Vin, Seq[Package.Id]]]]
  = for {
      _  <- Packages.exists2(name, version)
      fs <- listFiltersForPackage(name, version)
      vs <- vehicles.result.map(Xor.right(_))
    } yield
      makeFakeDependencyMap(name, version,
        vs.filter(query(fs.map(_.expression).map(parseValidFilter).foldLeft[FilterAST](True)(And))))
 */

  def makeFakeDependencyMap
    (name: Package.Name, version: Package.Version, vs: Seq[Vehicle])
      : Map[Vehicle.Vin, List[Package.Id]]
  = vs.map(vehicle => Map(vehicle.vin -> List(Package.Id(name, version))))
      .foldRight(Map[Vehicle.Vin, List[Package.Id]]())(_++_)
}
