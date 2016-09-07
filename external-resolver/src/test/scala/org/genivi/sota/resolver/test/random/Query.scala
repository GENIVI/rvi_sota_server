package org.genivi.sota.resolver.test.random

import akka.http.scaladsl.model.StatusCodes
import cats.state.{State, StateT}
import org.genivi.sota.resolver.resolve.ResolveFunctions
import org.genivi.sota.resolver.filters.{And, FilterAST, True}
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.filters.Filter
import FilterAST._
import org.genivi.sota.resolver.test._
import org.scalacheck.Gen
import Misc.{function0Instance, lift, monGen}
import org.genivi.sota.data.Device.DeviceId
import org.genivi.sota.data.{Device, PackageId}

import scala.annotation.tailrec
import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext
import cats.syntax.show._
import Device._
import org.genivi.sota.resolver.components.Component

sealed trait Query


final case object ListVehicles                        extends Query
final case class  ListVehiclesFor(cmp: Component)     extends Query
final case class  ListPackagesOnVehicle(veh: Device.Id) extends Query
final case class  ListPackagesFor(flt: Filter)        extends Query

final case object ListFilters                         extends Query
final case class  ListFiltersFor(pak: Package)        extends Query

final case class  Resolve(id: PackageId)              extends Query

final case object ListComponents                      extends Query
final case class  ListComponentsFor(veh: Device.Id)     extends Query

object Query extends
    VehicleRequestsHttp with
    PackageRequestsHttp with
    FilterRequestsHttp with
    ComponentRequestsHttp with
    PackageFilterRequestsHttp with
    ResolveRequestsHttp {

  def semQueries(qrs: List[Query])
                (implicit ec: ExecutionContext): State[RawStore, List[Semantics]] = {

    @tailrec def go(qrs0: List[Query], s0: RawStore, acc: List[Semantics]): (RawStore, List[Semantics]) =
      qrs0 match {
        case Nil          => (s0, acc.reverse)
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
    case ListVehicles =>
      State.get map (s => Semantics(Some(q),
        listVehicles, StatusCodes.OK,
        SuccessVehicles(s.devices.keySet)))

    case ListVehiclesFor(cmp) =>
      State.get map (s => Semantics(Some(q),
        listVehiclesHaving(cmp), StatusCodes.OK,
        SuccessVehicles(s.vehiclesHaving(cmp))))

    case ListComponents             =>
      State.get map (s => Semantics(Some(q),
        listComponents, StatusCodes.OK,
        SuccessComponents(s.components)))

    case ListComponentsFor(veh)     =>
      State.get map (s => Semantics(Some(q),
        listComponentsOnVehicle(veh), StatusCodes.OK,
        SuccessPartNumbers(s.devices(veh)._2.map(_.partNumber))))

    case ListPackagesOnVehicle(veh) =>
      State.get map (s => Semantics(Some(q),
        listPackagesOnVehicle(veh), StatusCodes.OK,
        SuccessPackageIds(s.devices(veh)._1.map(_.id))))

    case ListPackagesFor(flt) =>
      State.get map (s => Semantics(Some(q),
        listPackagesForFilter(flt), StatusCodes.OK,
        SuccessPackages(s.packagesHaving(flt))))

    case ListFilters                =>
      State.get map (s => Semantics(Some(q),
        listFilters, StatusCodes.OK,
        SuccessFilters(s.filters)))

    case ListFiltersFor(pak) =>
      State.get map (s => Semantics(Some(q),
        listFiltersForPackage(pak), StatusCodes.OK,
        SuccessFilters(s.packages(pak))))

    case Resolve(pkgId)             =>
      State.get map (s => Semantics(Some(q),
        resolve2(defaultNs, pkgId), StatusCodes.OK,
        SuccessVehicleMap(vehicleMap(s, pkgId))))

  }

  private def vehicleMap(s: RawStore, pkgId: PackageId): Map[Device.Id, List[PackageId]] = {

    // An AST for each filter associated to the given package.
    val filters: Set[FilterAST] =
      for (
        flt <- s.lookupFilters(pkgId).get
      ) yield parseValidFilter(flt.expression)

    // An AST AND-ing the filters associated to the given package.
    val expr: FilterAST =
      filters.toList.foldLeft[FilterAST](True)(And)

    // Apply the resulting filter to select vehicles.
    val devIds: Iterable[Device.Id] = for {
      (dev, (paks, comps)) <- s.devices
      pakIds = paks.map(_.id).toSeq
      compIds = comps.map(_.partNumber).toSeq
    // TODO This will not work
      entry2 = (DeviceId(dev.show), (pakIds, compIds))
      if query(expr)(entry2)
    } yield dev

    ResolveFunctions.makeFakeDependencyMap(pkgId, devIds.toSeq)
  }

  // scalastyle:off magic.number
  def genQuery: StateT[Gen, RawStore, Query] =
    for {
      s    <- StateT.stateTMonadState[Gen, RawStore].get
      vehs <- Store.numberOfVehicles
      pkgs <- Store.numberOfPackages
      cmps <- Store.numberOfComponents
      flts <- Store.numberOfFilters
      vcomp <- Store.numberOfVehiclesWithSomeComponent
      vpaks <- Store.numberOfVehiclesWithSomePackage
      pfilt <- Store.numberOfPackagesWithSomeFilter
      qry  <- lift(Gen.frequency(
        (10, Gen.const(ListVehicles)),
        (10, Gen.const(ListComponents)),
        ( 5, Gen.const(ListFilters)),

        (if (vehs > 0) 10 else 0, Gen.oneOf(
          Store.pickVehicle.runA(s).map(ListPackagesOnVehicle),
          Store.pickVehicle.runA(s).map(ListComponentsFor),
          Store.pickVehicle.runA(s).map(ListPackagesOnVehicle),
          Store.pickVehicle.runA(s).map(ListComponentsFor)
        )),

        (if (pfilt > 0) 10 else 0, Gen.oneOf(
          Store.pickPackageWithFilter.runA(s) map { case (pkg, flt) => ListPackagesFor(flt) },
          Store.pickPackageWithFilter.runA(s) map { case (pkg, flt) => ListFiltersFor(pkg)  }
        )),

        (if (vcomp > 0) 10 else 0,
          Store.pickVehicleWithComponent.runA(s) map { case (veh, cmp) => ListVehiclesFor(cmp) }),

        (if (pkgs > 0) 10 else 0,
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
