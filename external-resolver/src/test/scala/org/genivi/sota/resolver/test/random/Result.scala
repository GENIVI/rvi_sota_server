package org.genivi.sota.resolver.test.random

import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.rest.ErrorCode


sealed trait Result

final case class  Failure(c: ErrorCode)                                    extends Result
/**
  * A [[Semantics]] with [[Success]] makes [[org.genivi.sota.resolver.test.Random]]
  * check only the response's status code. To also check the response's body against expected results,
  * provide instead another Success case class that carries those expected results.
  */
final case object Success                                                  extends Result
final case class  SuccessVehicles(vehs : Set[Vehicle])                     extends Result
final case class  SuccessPackage (pkg  : Package)                          extends Result
final case class  SuccessPackages(pkgs : Set[PackageId])                   extends Result
final case class  SuccessFilters (filts: Set[Filter])                      extends Result
final case class  SuccessVehicleMap(m: Map[Vehicle.Vin, List[PackageId]])  extends Result {
  override def toString(): String = {
    val sb = new java.lang.StringBuilder("SuccessVehicleMap(")
    var isFirstElem = true
    for ( (vin, paks) <- m ) {
      if (!isFirstElem) { sb.append(", ") }
      isFirstElem = false
      sb.append(s"${vin.get} -> $paks")
    }
    sb.append(")")
    sb.toString
  }
}
