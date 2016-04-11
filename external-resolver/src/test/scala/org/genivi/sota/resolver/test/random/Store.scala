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

  // REPLACING

  def replacing(old: Filter, neu: Filter): RawStore = {
    var result = this
    val paksAffected = packagesHaving(old)
    for (p <- paksAffected) {
      val oldFilters = result.packages(p)
      val neuFilters = oldFilters - old + neu
      val neuPackages = result.packages.updated(p, neuFilters)
      result = result.copy(packages = neuPackages)
    }
    result
  }

  // REMOVING

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

  /**
    * Fails in case the given filter is associated to some package.
    * In that case,
    * [[org.genivi.sota.resolver.test.random.RawStore!.deassociating(Package,Filter):RawStore*]]
    * should have been invoked for each such package before attempting to remove the filter.
    */
  def removing(flt: Filter): RawStore = {
    val associatedTo = packagesHaving(flt)
    if (associatedTo.nonEmpty) {
      val paks = associatedTo.map(pkg => pkg.id.toString).mkString
      throw new RuntimeException(s"Filter $flt can't be removed, still installed on : $paks")
    }
    copy(filters = filters - flt)
  }

  // COMPONENTS FOR VEHICLES

  def installing(veh: Vehicle, cmpn: Component): RawStore = {
    val (paks, comps) = vehicles(veh)
    copy(vehicles = vehicles.updated(veh, (paks, comps + cmpn)))
  }

  def uninstalling(veh: Vehicle, cmpn: Component): RawStore = {
    val (paks, comps) = vehicles(veh)
    copy(vehicles = vehicles.updated(veh, (paks, comps - cmpn)))
  }

  // PACKAGES FOR VEHICLES

  def installing(veh: Vehicle, pkg: Package): RawStore = {
    val (paks, comps) = vehicles(veh)
    copy(vehicles = vehicles.updated(veh, (paks + pkg, comps)))
  }

  // FILTERS FOR PACKAGES

  def associating(pkg: Package, filt: Filter): RawStore = {
    val existing = packages(pkg)
    copy(packages = packages.updated(pkg, existing + filt))
  }

  def deassociating(pkg: Package, filt: Filter): RawStore = {
    val existing = packages(pkg)
    copy(packages = packages.updated(pkg, existing - filt))
  }

  // QUERIES

  private def toSet[E](elems: Iterable[E]): Set[E] = { elems.toSet }

  def vehiclesHaving(cmpn: Component): Set[Vehicle] = toSet {
    for (
      entry <- vehicles;
      (veh, (paks, comps)) = entry;
      if comps.contains(cmpn)
    ) yield veh
  }

  def vehiclesHaving(pkg: Package): Set[Vehicle] = toSet {
    for (
      entry <- vehicles;
      (veh, (paks, comps)) = entry;
      if paks.contains(pkg)
    ) yield veh
  }

  def packagesHaving(flt: Filter): Set[Package] = toSet {
    for (
      entry <- packages;
      (pkg, fs) = entry;
      if fs contains flt
    ) yield pkg
  }

  def packagesInUse(): Set[Package] = toSet {
    for (
      entry <- vehicles;
      (veh, (paks, comps)) = entry;
      pkg   <-  paks
    ) yield pkg
  }

  def componentsInUse(): Set[Component] = toSet {
    for (
      entry <- vehicles;
      (veh, (paks, comps)) = entry;
      cmpn  <-  comps
    ) yield cmpn
  }

  def filtersInUse(): Set[Filter] = toSet {
    for (
      entry <- packages;
      (pkg, fs) = entry;
      flt  <-  fs
    ) yield flt
  }

  def packagesUnused(): Set[Package] = { packages.keySet -- packagesInUse }

  def componentsUnused(): Set[Component] = { components -- componentsInUse }

  def filtersUnused(): Set[Filter] = { filters -- filtersInUse }

  // WELL-FORMEDNESS

  def isValid(): Boolean = {
    vehicles.forall { entry =>
      val (veh, (paks, comps)) = entry;
      paks.forall(packages.contains) && comps.forall(components.contains)
    } && packages.forall { entry =>
      val (pkg, fs) = entry
      fs.forall(filters.contains)
    }
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
      (_.vehicles.keys.size)

  def numberOfPackages: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.packages.keys.size)

  def numberOfFilters: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.filters.size)

}
