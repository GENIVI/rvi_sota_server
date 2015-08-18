/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.db

import org.genivi.sota.resolver.db.PackageFilters.listFiltersForPackage
import org.genivi.sota.resolver.db.Vehicles.vehicles
import org.genivi.sota.resolver.types.FilterParser.parseValidFilter
import org.genivi.sota.resolver.types.FilterQuery.query
import org.genivi.sota.resolver.types.{True, And}
import org.genivi.sota.resolver.types.{Vehicle, Package, FilterAST}
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


object Resolve {

  def resolve
    (name: Package.Name, version: Package.Version)
    (implicit ec: ExecutionContext)
      : DBIO[Seq[Vehicle]]
  = for {
    fs <- listFiltersForPackage(name, version)
    vs <- vehicles.result
  } yield
    vs.filter(query(fs.map(_.expression).map(parseValidFilter).foldLeft[FilterAST](True)(And)))

}
