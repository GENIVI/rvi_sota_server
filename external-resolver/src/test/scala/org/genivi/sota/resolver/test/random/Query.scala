package org.genivi.sota.resolver.test.random

import akka.http.scaladsl.model.StatusCodes
import cats.state.{State, StateT}
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.resolve.ResolveFunctions
import org.genivi.sota.resolver.filters.{And, FilterAST, True}
import FilterAST._
import org.genivi.sota.resolver.test.{FilterRequestsHttp, PackageRequestsHttp, ResolveRequestsHttp, VehicleRequestsHttp}
import org.genivi.sota.resolver.test.{SuccessFilters, SuccessPackages, SuccessVehicleMap, SuccessVehicles}
import org.scalacheck.Gen
import Misc.{function0Instance, lift, monGen}
import org.genivi.sota.data.{PackageId, Vehicle}


sealed trait Query

final case object ListVehicles                        extends Query
final case class  ListPackagesOnVehicle(veh: Vehicle) extends Query

final case object ListFilters                         extends Query

final case class  Resolve(id: PackageId)             extends Query


object Query extends
    VehicleRequestsHttp with
    PackageRequestsHttp with
    FilterRequestsHttp with
    ResolveRequestsHttp {

  def semQuery(q: Query): State[RawStore, Semantics] = q match {

    case ListVehicles               =>
      State.get map (s => Semantics(listVehicles, StatusCodes.OK,
        SuccessVehicles(s.vehicles.keySet)))

    case ListPackagesOnVehicle(veh) =>
      State.get map (s => Semantics(listPackagesOnVehicle(veh), StatusCodes.OK,
        SuccessPackages(s.vehicles(veh)._1.map(_.id))))

    case ListFilters                =>
      State.get map (s => Semantics(listFilters, StatusCodes.OK, SuccessFilters(s.filters)))

    case Resolve(pkgId)                =>

      def filters(s: RawStore, id: PackageId): Set[FilterAST] = {
        val fs = s.packages.map{ case (pkg, fs) => (pkg.id, fs) }
        fs(id).map(_.expression).map(parseValidFilter)
      }

      def expr(s: RawStore, id: PackageId): FilterAST =
        filters(s, id).toList.foldLeft[FilterAST](True)(And)

      State.get map (s => Semantics(resolve2(pkgId), StatusCodes.OK, SuccessVehicleMap(
        ResolveFunctions.makeFakeDependencyMap(pkgId,
          s.vehicles.keys.toList.map(v =>
              (v, (s.vehicles(v)._1.toSeq.map(_.id), s.vehicles(v)._2.toSeq.map(_.partNumber))))
           .filter(query(expr(s, pkgId))).map(_._1)))))

  }

  implicit val genQuery: StateT[Gen, RawStore, Query] =
    for {
      s    <- StateT.stateTMonadState[Gen, RawStore].get
      vehs <- Store.numberOfVehicles
      pkgs <- Store.numberOfPackages
      qry  <- lift(Gen.frequency(

        (10, Gen.const(ListVehicles)),

        (if (vehs > 0) 10 else 0,
          Store.pickVehicle.runA(s).map(ListPackagesOnVehicle(_))),

        (5,  Gen.const(ListFilters)),

        (if (pkgs > 0) 50 else 0,
          Store.pickPackage.runA(s).map(pkg => Resolve(pkg.id)))
      ))
    } yield qry

}
