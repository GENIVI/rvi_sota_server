package org.genivi.sota.resolver.test.random

import cats.state.StateT
import eu.timepit.refined.api.{Refined, Validate}
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.filters.Filter
import org.scalacheck.Gen
import Misc._
import org.genivi.sota.data.Device._
import cats.syntax.show.toShowOps
import org.genivi.sota.data.{Device, PackageId}
import org.genivi.sota.resolver.db.Package

import scala.collection.immutable.Iterable

// scalastyle:off number.of.methods
case class RawStore(
                     devices  : Map[Device.Id, (Set[Package], Set[Component])],
                     packages  : Map[Package, Set[Filter]],
                     filters   : Set[Filter],
                     components: Set[Component],
                     qryAggregates: QueryKindAggregates
) {

  /**
    * Used to communicate immutable data between the interpreter and the stats summarizer.
    */
  def withQrySems(sems: List[Semantics]): RawStore = {
    val justTheQueries = sems filter (_.isQuery)
    copy(qryAggregates = qryAggregates.merge(aggregateByQueryKind(justTheQueries)))
  }

  private def aggregateByQueryKind(qrySems: List[Semantics]): QueryKindAggregates = {
    val qsizes: List[(Query, Int)] = qrySems map { sem => (sem.original.get, sem.result.size.get)}
    val grouped: Map[Class[_], List[(Query, Int)]] = qsizes groupBy { case (q, s) => q.getClass }
    QueryKindAggregates(
      grouped mapValues { (lst: List[(Query, Int)]) =>
        QueryKindAggregate(lst.map(_._2).sum, lst.size)
      }
    )
  }

  // INSERTING

  def creating(veh: Device): RawStore = {
    copy(devices = devices.updated(veh.id, (Set.empty[Package], Set.empty[Component])))
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
    result = result.copy(filters = filters - old + neu)
    result
  }

  def replacing(old: Component, neu: Component): RawStore = {
    var result = this
    val vehsAffected = vehiclesHaving(old)
    for (v <- vehsAffected) {
      val (paks, oldComps) = result.devices(v)
      val neuComps = oldComps - old + neu
      val neuVehicles = result.devices.updated(v, (paks, neuComps))
      result = result.copy(devices = neuVehicles)
    }
    result = result.copy(components = components - old + neu)
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
      val vins = installedOn.map(_.show).mkString
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

  def installing(veh: Device.Id, cmpn: Component): RawStore = {
    val (paks, comps) = devices(veh)
    copy(devices = devices.updated(veh, (paks, comps + cmpn)))
  }

  def uninstalling(veh: Device.Id, cmpn: Component): RawStore = {
    val (paks, comps) = devices(veh)
    copy(devices = devices.updated(veh, (paks, comps - cmpn)))
  }

  // PACKAGES FOR VEHICLES

  def installing(veh: Device.Id, pkg: Package): RawStore = {
    val (paks, comps) = devices(veh)
    copy(devices = devices.updated(veh, (paks + pkg, comps)))
  }

  def uninstalling(veh: Device.Id, pkg: Package): RawStore = {
    val (paks, comps) = devices(veh)
    copy(devices = devices.updated(veh, (paks - pkg, comps)))
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

  /**
    * Vehicles with some package installed.
    */
  def vehiclesWithSomePackage: Map[Device.Id, Set[Package]] = {
    for (
      (veh, (packs, comps)) <- devices
      if packs.nonEmpty
    ) yield (veh, packs)
  }

  /**
    * Vehicles with some component installed.
    */
  def vehiclesWithSomeComponent: Map[Device.Id, Set[Component]] = {
    for (
      (veh, (packs, comps)) <- devices
      if comps.nonEmpty
    ) yield (veh, comps)
  }

  /**
    * Each pair indicates a Component that can be installed on a Vehicle.
    */
  def vehicleComponentPairsToInstall: Seq[(Device.Id, Component)] = {
    for (
      (veh, (packs, installedComps)) <- devices.toSeq;
      cmp <- components
      if !installedComps.contains(cmp)
    ) yield (veh, cmp)
  }

  def packagesWithSomeFilter: Map[Package, Set[Filter]] = {
    for (
      (p, fs) <- packages
      if fs.nonEmpty
    ) yield (p, fs)
  }

  /**
    * Each pair indicates a Filter that can be installed on a Package.
    */
  def packageFilterPairsToInstall: Seq[(Package, Filter)] = {
    for (
      (p, installedFilters) <- packages.toSeq;
      f <- filters
      if !installedFilters.contains(f)
    ) yield (p, f)
  }

  def vehiclesHaving(cmpn: Component): Set[Device.Id] = toSet {
    for (
      (veh, (paks, comps)) <- devices
      if comps.contains(cmpn)
    ) yield veh
  }

  def vehiclesHaving(pkg: Package): Set[Device.Id] = toSet {
    for (
      (veh, (paks, comps)) <- devices
      if paks.contains(pkg)
    ) yield veh
  }

  def packagesHaving(flt: Filter): Set[Package] = toSet {
    for (
      (pkg, fs) <- packages
      if fs contains flt
    ) yield pkg
  }

  def packagesInUse: Set[Package] = toSet {
    for (
      (veh, (paks, comps)) <- devices;
      pkg <-  paks
    ) yield pkg
  }

  def componentsInUse: Set[Component] = toSet {
    for (
      (veh, (paks, comps)) <- devices;
      cmpn  <-  comps
    ) yield cmpn
  }

  def filtersInUse: Set[Filter] = toSet {
    for (
      (pkg, fs) <- packages;
      flt  <-  fs
    ) yield flt
  }

  def packagesUnused: Set[Package] = { packages.keySet -- packagesInUse }

  def componentsUnused: Set[Component] = { components -- componentsInUse }

  def filtersUnused: Set[Filter] = { filters -- filtersInUse }

  // LOOKUPS

  private def toHead[A](elems: Iterable[A]): Option[A] = elems.headOption

  def lookupFilters(id: PackageId): Option[Set[Filter]] = toHead {
    for (
      (pkg, fs) <- packages
      if pkg.id == id
    ) yield fs
  }

  def lookupPkgsComps(vin: Device.Id): Option[(Set[Package], Set[Component])] = toHead {
    for (
      (veh, (paks, comps)) <- devices
      if veh == vin
    ) yield (paks, comps)
  }

  def lookupPackages(vin: Device.Id): Option[Set[Package]] =
    lookupPkgsComps(vin).map(_._1)

  def lookupComponents(vin: Device.Id): Option[Set[Component]] =
    lookupPkgsComps(vin).map(_._2)

  // WELL-FORMEDNESS

  def isValid: Boolean = {
    devices.forall { entry =>
      val (_, (paks, comps)) = entry
      paks.forall(packages.contains) && comps.forall(components.contains)
    } && packages.forall { entry =>
      val (_, fs) = entry
      fs.forall(filters.contains)
    }
  }

}

object Store {

  val initRawStore: RawStore =
    RawStore(Map(), Map(), Set(), Set(), QueryKindAggregates(Map.empty))

  case class ValidStore()

  type Store = Refined[RawStore, ValidStore]

  implicit val validStore : Validate.Plain[RawStore, ValidStore] = Validate.fromPredicate(
    s => s.isValid,
    s => s"($s isn't a valid state)",
    ValidStore()
  )

  def pick[T](elems: collection.Iterable[T]): T = {
    // avoiding elems.toVector thus space-efficient
    val n = util.Random.nextInt(elems.size)
    val it = elems.iterator.drop(n)
    it.next
  }

  def pickVehicle: StateT[Gen, RawStore, Device.Id] =
    for {
      s    <- StateT.stateTMonadState[Gen, RawStore].get
      vehs =  s.devices.keys
    } yield pick(vehs)

  def pickPackage: StateT[Gen, RawStore, Package] =
    for {
      s    <- StateT.stateTMonadState[Gen, RawStore].get
      pkgs =  s.packages.keys
    } yield pick(pkgs)

  def pickFilter: StateT[Gen, RawStore, Filter] =
    for {
      s     <- StateT.stateTMonadState[Gen, RawStore].get
      filts =  s.filters
    } yield pick(filts)

  def pickUnusedFilter: StateT[Gen, RawStore, Filter] =
    for {
      s     <- StateT.stateTMonadState[Gen, RawStore].get
      uflts =  s.filtersUnused
    } yield pick(uflts)

  def pickComponent: StateT[Gen, RawStore, Component] =
    for {
      s    <- StateT.stateTMonadState[Gen, RawStore].get
      comps =  s.components
    } yield pick(comps)

  def pickUnusedComponent: StateT[Gen, RawStore, Component] =
    for {
      s    <- StateT.stateTMonadState[Gen, RawStore].get
      comps = s.componentsUnused
    } yield pick(comps)

  def pickVehicleWithComponent: StateT[Gen, RawStore, (Device.Id, Component)] =
    for {
      s    <- StateT.stateTMonadState[Gen, RawStore].get
      vcs   = s.vehiclesWithSomeComponent
    } yield {
      val (veh, comps) = pick(vcs)
      (veh, pick(comps))
    }

  def pickVehicleComponentPairToInstall: StateT[Gen, RawStore, (Device.Id, Component)] =
    for {
      s     <- StateT.stateTMonadState[Gen, RawStore].get
      pairs  = s.vehicleComponentPairsToInstall
    } yield {
      pick(pairs)
    }

  def pickVehicleWithPackage: StateT[Gen, RawStore, (Device.Id, Package)] =
    for {
      s    <- StateT.stateTMonadState[Gen, RawStore].get
      vps   = s.vehiclesWithSomePackage
    } yield {
      val (veh, paks) = pick(vps)
      (veh, pick(paks))
    }

  def pickPackageWithFilter: StateT[Gen, RawStore, (Package, Filter)] =
    for {
      s    <- StateT.stateTMonadState[Gen, RawStore].get
      pfs   = s.packagesWithSomeFilter
    } yield {
      val (veh, fs) = pick(pfs)
      (veh, pick(fs))
    }

  def pickPackageFilterPairToInstall: StateT[Gen, RawStore, (Package, Filter)] =
    for {
      s     <- StateT.stateTMonadState[Gen, RawStore].get
      pairs  = s.packageFilterPairsToInstall
    } yield {
      pick(pairs)
    }

  def numberOfVehicles: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.devices.keys.size)

  def numberOfPackages: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.packages.keys.size)

  def numberOfFilters: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.filters.size)

  def numberOfUnusedFilters: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.filtersUnused.size)

  def numberOfComponents: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.components.size)

  def numberOfUnusedComponents: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.componentsUnused.size)

  def numberOfVehiclesWithSomePackage: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.vehiclesWithSomePackage.size)

  def numberOfVehiclesWithSomeComponent: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.vehiclesWithSomeComponent.size)

  def numberOfPackagesWithSomeFilter: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.packagesWithSomeFilter.size)

  def numberOfVehicleComponentPairsToInstall: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.vehicleComponentPairsToInstall.size)

  def numberOfPackageFilterPairsToInstall: StateT[Gen, RawStore, Int] =
    StateT.stateTMonadState[Gen, RawStore].get map
      (_.packageFilterPairsToInstall.size)
}
// scalastyle:on
