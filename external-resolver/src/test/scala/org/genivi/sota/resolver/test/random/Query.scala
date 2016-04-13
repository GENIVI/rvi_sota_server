package org.genivi.sota.resolver.test.random

import akka.http.scaladsl.model.StatusCodes
import cats.state.{State, StateT}
import org.genivi.sota.resolver.resolve.ResolveFunctions
import org.genivi.sota.resolver.filters.{And, FilterAST, True}
import FilterAST._
import org.genivi.sota.resolver.test.{FilterRequestsHttp, PackageRequestsHttp, ResolveRequestsHttp, VehicleRequestsHttp}
import org.scalacheck.Gen
import Misc.{lift, monGen, function0Instance}
import org.genivi.sota.data.Vehicle.Vin
import org.genivi.sota.data.{PackageId, Vehicle}

import scala.annotation.tailrec
import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext

sealed trait Query

final case object ListVehicles                        extends Query
final case class  ListPackagesOnVehicle(veh: Vehicle) extends Query

final case object ListFilters                         extends Query

final case class  Resolve(id: PackageId)              extends Query

// TODO Query: list components
// TODO Query: vehicles having component
// TODO Query: components for vehicle
// TODO Query: filters for package

object Query extends
    VehicleRequestsHttp with
    PackageRequestsHttp with
    FilterRequestsHttp with
    ResolveRequestsHttp {

  def semQueries(qrs: List[Query])
                (implicit ec: ExecutionContext): State[RawStore, List[Semantics]] = {

    @tailrec def go(qrs0: List[Query], s0: RawStore, acc: List[Semantics]): (RawStore, List[Semantics]) =
      qrs0 match {
        case Nil           => (s0, acc.reverse)
        case (qr :: qrs1) =>
          val (s1, r) = semQuery(qr).run(s0).run
          go(qrs1, s1, r :: acc)
      }

    State.get.flatMap { s0 =>
      val (s1, sems) = go(qrs, s0, List())
      State.set(s1).flatMap(_ => State.pure(sems))
    }

  }

  def semQuery(q: Query): State[RawStore, Semantics] = q match {

    case ListVehicles               =>
      State.get map (s => Semantics(listVehicles, StatusCodes.OK,
        SuccessVehicles(s.vehicles.keySet)))

    case ListPackagesOnVehicle(veh) =>
      State.get map (s => Semantics(listPackagesOnVehicle(veh), StatusCodes.OK,
        SuccessPackages(s.vehicles(veh)._1.map(_.id))))

    case ListFilters                =>
      State.get map (s => Semantics(listFilters, StatusCodes.OK,
        SuccessFilters(s.filters)))

    case Resolve(pkgId)                =>
      State.get map (s => Semantics(resolve2(pkgId), StatusCodes.OK,
        SuccessVehicleMap(vehicleMap(s, pkgId))))

  }

  private def vehicleMap(s: RawStore, pkgId: PackageId): Map[Vin, List[PackageId]] = {

    // An AST for each filter associated to the given package.
    val filters: Set[FilterAST] =
      for (
        flt <- s.lookupFilters(pkgId).get
      ) yield parseValidFilter(flt.expression)

    // An AST AND-ing the filters associated to the given package.
    val expr: FilterAST =
      filters.toList.foldLeft[FilterAST](True)(And)

    // Apply the resulting filter to select vehicles.
    val vehs: Iterable[Vehicle] = for (
      (veh, (paks, comps)) <- s.vehicles;
      pakIds = paks.map(_.id).toSeq;
      compIds = comps.map(_.partNumber).toSeq;
      entry2 = (veh, (pakIds, compIds));
      if query(expr)(entry2)
    ) yield veh

    ResolveFunctions.makeFakeDependencyMap(pkgId, vehs.toSeq)
  }

  // scalastyle:off magic.number
  def genQuery: StateT[Gen, RawStore, Query] =
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
  // scalastyle:on

  def genQueries(n: Int)
                (implicit ec: ExecutionContext): StateT[Gen, RawStore, List[Query]] = {
    if (n < 1) throw new IllegalArgumentException
    for {
      q  <- genQuery
      qs <- if (n == 1) genQuery.map(List(_)) else genQueries(n - 1)
    } yield q :: qs
  }

}
