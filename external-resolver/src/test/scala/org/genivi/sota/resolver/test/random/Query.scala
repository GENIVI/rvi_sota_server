package org.genivi.sota.resolver.test.random

import akka.http.scaladsl.model.StatusCodes
import cats.state.{State, StateT}
import org.genivi.sota.resolver.vehicles.Vehicle
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.test.{VehicleRequestsHttp, PackageRequestsHttp, FilterRequestsHttp}
import org.genivi.sota.resolver.test.{Result, Success, Failure, SuccessVehicles, SuccessPackages, SuccessFilters}
import org.scalacheck.Gen
import Misc.{lift, monGen, function0Instance}


sealed trait Query

final case object ListVehicles                        extends Query
final case class  ListPackagesOnVehicle(veh: Vehicle) extends Query

final case object ListFilters                         extends Query

final case class  Resolve(pkg: Package)               extends Query


object Query extends
    VehicleRequestsHttp with
    PackageRequestsHttp with
    FilterRequestsHttp {

  def semQuery(q: Query): State[RawStore, Semantics] = q match {

    case ListVehicles               =>
      State.get map (s => Semantics(listVehicles, StatusCodes.OK,
        SuccessVehicles(s.vehicles.keySet)))

    case ListPackagesOnVehicle(veh) =>
      State.get map (s => Semantics(listPackagesOnVehicle(veh), StatusCodes.OK,
        SuccessPackages(s.vehicles(veh)._1.map(_.id))))

    case ListFilters                =>
      State.get map (s => Semantics(listFilters, StatusCodes.OK, SuccessFilters(s.filters)))

  }

  implicit val genQuery: StateT[Gen, RawStore, Query] =
    for {
      s       <- StateT.stateTMonadState[Gen, RawStore].get
      hasVehs <- Store.hasVehicles
      qry     <- lift(Gen.frequency(
        (10, Gen.const(ListVehicles)),
        (if (hasVehs) 10 else 0, Store.pickVehicle.runA(s).map(ListPackagesOnVehicle(_))),
        (5,  Gen.const(ListFilters))
        ))
    } yield qry

}
