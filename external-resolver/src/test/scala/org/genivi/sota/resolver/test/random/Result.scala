package org.genivi.sota.resolver.test

import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.rest.ErrorCode


sealed trait Result

final case class  Failure(c: ErrorCode)                                    extends Result
final case object Success                                                  extends Result
final case class  SuccessVehicles(vehs : Set[Vehicle])                     extends Result
final case class  SuccessPackage (pkg  : Package)                          extends Result
final case class  SuccessPackages(pkgs : Set[PackageId])                  extends Result
final case class  SuccessFilters (filts: Set[Filter])                      extends Result
final case class  SuccessVehicleMap(m: Map[Vehicle.Vin, List[PackageId]]) extends Result
