package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCode
import org.genivi.sota.resolver.vehicles.Vehicle
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.filters.Filter

sealed trait Result

final case class  Failure(c: StatusCode)                  extends Result
final case object Success                                 extends Result
final case class  SuccessVehicles(vehs : Set[Vehicle])    extends Result
final case class  SuccessPackage (pkg  : Package)         extends Result
final case class  SuccessPackages(pkgs : Set[Package.Id]) extends Result
final case class  SuccessFilters (filts: Set[Filter])     extends Result
