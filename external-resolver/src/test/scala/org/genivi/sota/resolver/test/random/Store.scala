package org.genivi.sota.resolver.test.random

import cats.state.StateT
import eu.timepit.refined.api.{Refined, Validate}
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.resolver.packages.Package
import org.scalacheck.Gen
import Misc._
import org.genivi.sota.data.Vehicle

import scala.collection.immutable.Iterable


case class RawStore(
  vehicles  : Map[Vehicle, (Set[Package], Set[Component])],
  packages  : Map[Package, Set[Filter]],
  filters   : Set[Filter],
  components: Set[Component]
) {

  // INSERTING

  def creating(veh: Vehicle): RawStore = {
    copy(vehicles = vehicles.updated(veh, (Set.empty[Package], Set.empty[Component])))
  }

  def creating(pkg: Package): RawStore = {
    copy(packages = packages + (pkg -> Set.empty))
  }

  def creating(cmpn: Component): RawStore = {
    copy(components = components + cmpn)
  }

  def creating(filter: Filter): RawStore = {
    copy(filters = filters + filter)
  }

  // MANIPULATING PACKAGES AND COMPONENTS

  /**
    * Fails in case the given component is installed on any vin.
    * In that case,
    * [[org.genivi.sota.resolver.test.random.RawStore!.uninstalling(Vehicle,Component):RawStore*]]
    * should have been invoked for each such vin before attempting to remove the component.
    */
  def removing(cmpn: Component): RawStore = {
    val installedOn = vehiclesHaving(cmpn)
    if (installedOn.nonEmpty) {
      val vins = installedOn.map(veh => veh.vin.get).mkString
      throw new RuntimeException(s"Component $cmpn can't be removed, still installed in : $vins")
    }
    copy(components = components - cmpn)
  }

  def installing(veh: Vehicle, cmpn: Component): RawStore = {
    val (paks, comps) = vehicles(veh)
    copy(vehicles = vehicles.updated(veh, (paks, comps + cmpn)))
  }

  def uninstalling(veh: Vehicle, cmpn: Component): RawStore = {
    val (paks, comps) = vehicles(veh)
    copy(vehicles = vehicles.updated(veh, (paks, comps - cmpn)))
  }

  def installing(veh: Vehicle, pkg: Package): RawStore = {
    val (paks, comps) = vehicles(veh)
    copy(vehicles = vehicles.updated(veh, (paks + pkg, comps)))
  }

  def associating(pkg: Package, filt: Filter): RawStore = {
    val existing = packages(pkg)
    copy(packages = packages.updated(pkg, existing + filt))
  }

  // QUERIES

  def vehiclesHaving(cmpn: Component): Iterable[Vehicle] = {
    for (
      entry <- vehicles;
      (veh, (paks, comps)) = entry;
      if comps.contains(cmpn)
    ) yield veh
  }

  def allInstalledPackages(): Iterable[Package] = {
    for (
      entry <- vehicles;
      pkg   <-  entry._2._1
    ) yield pkg
  }

  def allInstalledComponents(): Iterable[Component] = {
    for (
      entry <- vehicles;
      cmpn  <-  entry._2._2
    ) yield cmpn
  }

  def allAssociatedFilters(): Iterable[Filter] = {
    for (
      entry <- packages;
      flt  <-  entry._2
    ) yield flt
  }

  def isValid(): Boolean = {
    allInstalledPackages().forall(pkg => packages.contains(pkg)) &&
      allInstalledComponents().forall(cmpn => components.contains(cmpn)) &&
      allAssociatedFilters().forall(flt => filters.contains(flt))
  }

}

object Store {

  val initRawStore: RawStore =
    RawStore(Map(), Map(), Set(), Set())

  case class ValidStore()

  type Store = Refined[RawStore, ValidStore]

  implicit val validStore : Validate.Plain[RawStore, ValidStore] = Validate.fromPredicate(
    s => s.isValid(),
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
