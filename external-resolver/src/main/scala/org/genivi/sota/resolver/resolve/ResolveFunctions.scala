/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.resolve

import org.genivi.sota.data.{PackageId, Vehicle}

object ResolveFunctions {

  def makeFakeDependencyMap(pkgId: PackageId, vs: Seq[Vehicle.Vin])
      : Map[Vehicle.Vin, List[PackageId]] =
    vs.map(vin => Map(vin -> List(pkgId)))
      .foldRight(Map[Vehicle.Vin, List[PackageId]]())(_++_)

}
