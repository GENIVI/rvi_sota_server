package org.genivi.sota.resolver.test.random

import cats.state.StateT
import eu.timepit.refined.api.{Refined, Validate}
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.resolver.packages.Package
import org.scalacheck.Gen
import Misc._
import org.genivi.sota.data.Vehicle


case class RawStore(
  vehicles  : Map[Vehicle, (Set[Package], Set[Component])],
  packages  : Map[Package, Set[Filter]],
  filters   : Set[Filter],
  components: Set[Component]
)

object Store {

  val initRawStore: RawStore =
    RawStore(Map(), Map(), Set(), Set())

  case class ValidStore()

  type Store = Refined[RawStore, ValidStore]

  implicit val validStore : Validate.Plain[RawStore, ValidStore] = Validate.fromPredicate(
    s => s.vehicles.values.map(_._1).forall(_.subsetOf(s.packages.keySet))
      && s.vehicles.values.map(_._2).forall(_.subsetOf(s.components))
      && s.packages.values.forall(_.subsetOf(s.filters)),
    s => s"($s isn't a valid state)",
    ValidStore()
  )

  def pickVehicle: StateT[Gen, RawStore, Vehicle] =
    for {
      s    <- StateT.stateTMonadState[Gen, RawStore].get
      vehs =  s.vehicles.keys.toVector
      n    <- lift(Gen.choose(0, vehs.length - 1))
    } yield vehs(n)

  def pickPackage: StateT[Gen, RawStore, Package] =
    for {
      s    <- StateT.stateTMonadState[Gen, RawStore].get
      pkgs =  s.packages.keys.toVector
      n    <- lift(Gen.choose(0, pkgs.length - 1))
    } yield pkgs(n)

  def pickFilter: StateT[Gen, RawStore, Filter] =
    for {
      s     <- StateT.stateTMonadState[Gen, RawStore].get
      filts =  s.filters.toVector
      n     <- lift(Gen.choose(0, filts.length - 1))
    } yield filts(n)

  def numberOfVehicles: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.vehicles.keys.toList.length)

  def numberOfPackages: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.packages.keys.toList.length)

  def numberOfFilters: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.filters.toList.length)

}
