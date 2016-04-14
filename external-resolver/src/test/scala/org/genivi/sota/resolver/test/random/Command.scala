package org.genivi.sota.resolver.test.random

import akka.http.scaladsl.model.{HttpRequest, StatusCode, StatusCodes}
import cats.state.{State, StateT}
import org.genivi.sota.data.Namespaces
import org.genivi.sota.data.{Vehicle, VehicleGenerators}
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.resolver.packages.{Package, PackageFilter}
import org.genivi.sota.resolver.test._
import org.genivi.sota.rest.ErrorCodes
import org.scalacheck.Gen
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

import Misc._


sealed trait Command

final case class AddVehicle    (veh: Vehicle)               extends Command

final case class AddPackage    (pkg: Package)               extends Command
final case class InstallPackage(veh: Vehicle, pkg: Package) extends Command

final case class AddFilter         (filt: Filter)               extends Command
final case class EditFilter        (old : Filter, neu: Filter)  extends Command {
  // restriction imposed by the endpoint allowing only a filter's expression (but not name) to be updated.
  if (old.name.get != neu.name.get) throw new IllegalArgumentException
}
final case class RemoveFilter      (filt: Filter)               extends Command
final case class AddFilterToPackage(pkg: Package, filt: Filter) extends Command
// TODO final case class RemoveFilterForPackage(pkg: Package, filt: Filter) extends Command

final case class AddComponent   (cmpn: Component) extends Command
// TODO final case class EditComponent        (old : Component, neu: Component)  extends Command
final case class RemoveComponent(cmpn: Component) extends Command
final case class InstallComponent  (veh: Vehicle, cmpn: Component) extends Command
final case class UninstallComponent(veh: Vehicle, cmpn: Component) extends Command


object Command extends
    VehicleRequestsHttp with
    PackageRequestsHttp with
    FilterRequestsHttp  with
    ComponentRequestsHttp with
    PackageFilterRequestsHttp with
    Namespaces {

  type SemCommand = (HttpRequest, StatusCode, Result)

  def semCommands(cmds: List[Command])
                 (implicit ec: ExecutionContext): State[RawStore, List[Semantics]] = {

    @tailrec def go(cmds0: List[Command], s0: RawStore, acc: List[Semantics]): (RawStore, List[Semantics]) =
      cmds0 match {
        case Nil           => (s0, acc.reverse)
        case (cmd :: cmds1) =>
          val (s1, r) = semCommand(cmd).run(s0).run
          go(cmds1, s1, r :: acc)
      }

    State.get.flatMap { s0 =>
      val (s1, sems) = go(cmds, s0, List())
      State.set(s1).flatMap(_ => State.pure(sems))
    }

  }

  // scalastyle:off cyclomatic.complexity
  // scalastyle:off method.length
  def semCommand(cmd: Command)
                (implicit ec: ExecutionContext): State[RawStore, Semantics] = cmd match {

    case AddVehicle(veh)          =>
      for {
        s <- State.get
        _ <- State.set(s.creating(veh))
      } yield Semantics(addVehicle(veh.vin), StatusCodes.NoContent, Success)

    case AddPackage(pkg)          =>
      for {
        s <- State.get
        _ <- State.set(s.creating(pkg))
      } yield Semantics(addPackage(pkg), StatusCodes.OK, SuccessPackage(pkg))

    case InstallPackage(veh, pkg) =>
      for {
        s <- State.get
        _ <- State.set(s.installing(veh, pkg))
      } yield Semantics(installPackage(veh.vin, pkg.id.name.get, pkg.id.version.get), StatusCodes.OK, Success)
                                       // XXX: move gets inwards...

    case AddFilter(filt)               =>
      for {
        s <- State.get
        _ <- State.set(s.creating(filt))
      } yield Semantics(addFilter2(filt), StatusCodes.OK, Success)

    case EditFilter(old, neu)          =>
      for {
        s <- State.get
        _ <- State.set(s.replacing(old, neu))
      } yield Semantics(updateFilter(neu), StatusCodes.OK, Success)

    case RemoveFilter(filt)            =>
      for {
        s       <- State.get
        _       <- State.set(s.removing(filt))
        success =  s.filtersInUse.isEmpty
      } yield {
        val req = deleteFilter(filt)
        if (success) { Semantics(req, StatusCodes.OK, Success) }
        else         { Semantics(req, StatusCodes.Conflict, Failure(ErrorCodes.DuplicateEntry)) }
      }

    case AddFilterToPackage(pkg, filt) =>
      for {
        s       <- State.get
        _       <- State.set(s.associating(pkg, filt))
        success =  !s.packages(pkg).contains(filt)
      } yield {
        val req = addPackageFilter2(PackageFilter(pkg.namespace, pkg.id.name, pkg.id.version, filt.name))
        if (success) { Semantics(req, StatusCodes.OK, Success) }
        else         { Semantics(req, StatusCodes.Conflict, Failure(ErrorCodes.DuplicateEntry)) }
      }

    // TODO case RemoveFilterForPackage(pkg: Package, filt: Filter)

    case AddComponent(cmpn)     =>
      for {
        s <- State.get
        _ <- State.set(s.creating(cmpn))
        isDuplicatePK = s.components.exists(_.partNumber == cmpn.partNumber)
      } yield {
        val req = addComponent(cmpn.partNumber, cmpn.description)
        if (isDuplicatePK) { Semantics(req, StatusCodes.Conflict, Failure(ErrorCodes.DuplicateEntry)) }
        else               { Semantics(req, StatusCodes.OK, Success) }
      }

    // TODO case EditComponent(old: Component, neu: Component)

    case RemoveComponent(cmpn)      =>
      for {
        s <- State.get
        _ <- State.set(s.removing(cmpn))
      } yield Semantics(
        deleteComponent(cmpn.partNumber),
        StatusCodes.OK, Success) // whether it was there or not, OK is the reply

    case InstallComponent(veh, cmpn)     =>
      for {
        s <- State.get
        _ <- State.set(s.installing(veh, cmpn))
        isDuplicatePK = s.vehicles(veh)._2.contains(cmpn)
      } yield {
        val req = installComponent(veh, cmpn)
        if (isDuplicatePK) { Semantics(req, StatusCodes.Conflict, Failure(ErrorCodes.DuplicateEntry)) }
        else               { Semantics(req, StatusCodes.OK, Success) }
      }

    case UninstallComponent(veh, cmpn)   =>
      for {
        s <- State.get
        _ <- State.set(s.uninstalling(veh, cmpn))
      } yield Semantics(
        uninstallComponent(veh, cmpn),
        StatusCodes.OK, Success) // whether already uninstalled or not, OK is the reply

  }
  // scalastyle:on

  private def genCommandAddVehicle(): Gen[AddVehicle] =
    VehicleGenerators.genVehicle.map(AddVehicle(_))

  private def genCommandAddPackage(): Gen[AddPackage] =
    PackageGenerators.genPackage.map(AddPackage(_))

  private def genCommandAddFilter(s: RawStore): Gen[AddFilter] =
    FilterGenerators.genFilter(s.packages.keys.toList, s.components.toList)
      .map(AddFilter(_))

  private def genCommandAddComponent(s: RawStore): Gen[AddComponent] =
    ComponentGenerators.genComponent.map(AddComponent(_))

  private def genCommandInstallPackage(s: RawStore): Gen[InstallPackage] =
    for {
      veh <- Store.pickVehicle.runA(s)
      pkg <- Store.pickPackage.runA(s)
    } yield InstallPackage(veh, pkg)

  private def genCommandInstallComponent(s: RawStore): Gen[InstallComponent] =
    for {
      veh <- Store.pickVehicle.runA(s)
      cmp <- Store.pickComponent.runA(s)
    } yield InstallComponent(veh, cmp)

  private def genCommandUninstallComponent(s: RawStore): Gen[UninstallComponent] =
    for {
      (veh, cmp) <- Store.pickVehicleWithComponent.runA(s)
    } yield UninstallComponent(veh, cmp)

  private def genCommandAddFilterToPackage(s: RawStore): Gen[AddFilterToPackage] =
    for {
      pkg  <- Store.pickPackage.runA(s)
      filt <- Store.pickFilter.runA(s)
    } yield AddFilterToPackage(pkg, filt)

  private def genCommandEditFilter(s: RawStore): Gen[EditFilter] =
    for {
      fltOld <- Store.pickFilter.runA(s)
      fltNu0 <- FilterGenerators.genFilter(s.packages.keys.toList, s.components.toList)
      fltNu1  = Filter(defaultNs, fltOld.name, fltNu0.expression)
    } yield EditFilter(fltOld, fltNu1)

  // scalastyle:off cyclomatic.complexity
  // scalastyle:off magic.number
  def genCommand(implicit ec: ExecutionContext): StateT[Gen, RawStore, Command] =
    for {
      s     <- StateT.stateTMonadState[Gen, RawStore].get
      vehs  <- Store.numberOfVehicles
      pkgs  <- Store.numberOfPackages
      filts <- Store.numberOfFilters
      comps <- Store.numberOfComponents
      vcomp <- Store.numberOfVehiclesWithSomeComponent
      cmd   <- lift(Gen.frequency(

        // If there are few vehicles, packages or filters in the world,
        // then generate some with high probability.

        (if (vehs <= 10) 100 else 1, genCommandAddVehicle),

        (if (pkgs <= 5)  100 else 1, genCommandAddPackage),

        (if (filts <= 3) 20 else 1, genCommandAddFilter(s)),

        (if (comps <= 3) 20 else 1, genCommandAddComponent(s)),

        // If there are vehicles and packages, then install some
        // packages on the vehicles with high probability.
        (if (vehs > 0 && pkgs > 0) 100 else 0, genCommandInstallPackage(s)),

        // If there are vehicles and components
        (if (vehs > 0 && comps > 0) 100 else 0, genCommandInstallComponent(s)),

        // TODO fix VehicleRepository.uninstallComponent (if (vcomp > 0) 10 else 0, genCommandUninstallComponent(s)),

        // If there are packages and filters, install some filter to some package.
        (if (pkgs > 0 && filts > 0) 50 else 0, genCommandAddFilterToPackage(s))

        // TODO EditFilter        (old : Filter, neu: Filter)
        // TODO RemoveFilter      (filt: Filter)

        // TODO EditComponent     (old : Component, neu: Component)
        // TODO RemoveComponent   (cmpn: Component)

      ))
      _   <- StateT.stateTMonadState(monGen).set(semCommand(cmd).runS(s).run)
    } yield cmd
  // scalastyle:on

  def genCommands(n: Int)
                 (implicit ec: ExecutionContext): StateT[Gen, RawStore, List[Command]] = {
    if (n < 1) throw new IllegalArgumentException
    for {
      cmd  <- genCommand
      cmds <- if (n == 1) genCommand.map(List(_)) else genCommands(n - 1)
    } yield cmd :: cmds
  }

}
