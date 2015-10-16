/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.resolve

import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.vehicles.Vehicle


object ResolveFunctions {

  def makeFakeDependencyMap
    (pkgId: Package.Id, vs: Seq[Vehicle])
      : Map[Vehicle.Vin, List[Package.Id]] =
    vs.map(vehicle => Map(vehicle.vin -> List(pkgId)))
      .foldRight(Map[Vehicle.Vin, List[Package.Id]]())(_++_)

}
