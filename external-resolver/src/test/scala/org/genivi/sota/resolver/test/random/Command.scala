package org.genivi.sota.resolver.test.random

import akka.http.scaladsl.model.{HttpRequest, StatusCode, StatusCodes}
import cats.state.{State, StateT}
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.resolver.packages.{Package, PackageFilter}
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.test._
import org.genivi.sota.rest.ErrorCodes
import org.scalacheck.{Arbitrary, Gen}

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

import Misc._
import org.genivi.sota.data.{Vehicle, VehicleGenerators}


sealed trait Command

final case class AddVehicle    (veh: Vehicle)               extends Command

final case class AddPackage    (pkg: Package)               extends Command
final case class InstallPackage(veh: Vehicle, pkg: Package) extends Command

final case class AddFilter         (filt: Filter)               extends Command
final case class EditFilter        (old : Filter, neu: Filter)  extends Command
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
    PackageFilterRequestsHttp {

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

  // scalastyle:off
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

    case EditFilter(old, neu)          => ???

    case RemoveFilter(filt)            =>
      for {
        s       <- State.get
        _       <- State.set(s.removing(filt))
        success =  s.filtersInUse.isEmpty
      } yield
        if (success)
          Semantics(deleteFilter(filt), StatusCodes.OK, Success)
        else
          Semantics(deleteFilter(filt), StatusCodes.Conflict, Failure(ErrorCodes.DuplicateEntry)) // TODO Error Code

    case AddFilterToPackage(pkg, filt) =>
      for {
        s       <- State.get
        _       <- State.set(s.associating(pkg, filt))
        success =  !s.packages(pkg).contains(filt)
      } yield
          if (success)
            Semantics(addPackageFilter2(PackageFilter(pkg.id.name, pkg.id.version, filt.name)),
              StatusCodes.OK, Success)
          else
            Semantics(addPackageFilter2(PackageFilter(pkg.id.name, pkg.id.version, filt.name)),
              StatusCodes.Conflict, Failure(ErrorCodes.DuplicateEntry))

    case AddComponent(cmpn)     =>
      for {
        s <- State.get
        _ <- State.set(s.creating(cmpn))
      } yield Semantics(
        addComponent(cmpn.partNumber, cmpn.description),
        StatusCodes.OK, Success) // duplicate or not, OK is the reply

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
      } yield Semantics(
        installComponent(veh, cmpn),
        StatusCodes.OK, Success) // whether already installed or not, OK is the reply

    case UninstallComponent(veh, cmpn)   =>
      for {
        s <- State.get
        _ <- State.set(s.uninstalling(veh, cmpn))
      } yield Semantics(
        uninstallComponent(veh, cmpn),
        StatusCodes.OK, Success) // whether already uninstalled or not, OK is the reply

  }
  // scalastyle:on

  // scalastyle:off
  def genCommand(implicit ec: ExecutionContext): StateT[Gen, RawStore, Command] =
    for {
      s     <- StateT.stateTMonadState[Gen, RawStore].get
      vehs  <- Store.numberOfVehicles
      pkgs  <- Store.numberOfPackages
      filts <- Store.numberOfFilters
      cmd   <- lift(Gen.frequency(

        // If there are few vehicles, packages or filters in the world,
        // then generate some with high probability.

        (if (0 <= vehs && vehs <= 10) 100 else 1,
          VehicleGenerators.genVehicle.map(AddVehicle(_))),

        (if (0 <= pkgs && pkgs <= 5)  100 else 1,
          PackageGenerators.genPackage.map(AddPackage(_))),

        (if (0 <= filts && filts <= 3) 20 else 1,
          FilterGenerators.genFilter(s.packages.keys.toList, s.components.toList)
                .map(AddFilter(_))),

        // If there are vehicles and packages, then install some
        // packages on the vehicles with high probability.
        (if (vehs > 0 && pkgs > 0) 100 else 0,
          for {
            veh <- Store.pickVehicle.runA(s)
            pkg <- Store.pickPackage.runA(s)
          } yield InstallPackage(veh, pkg)),

        // If there are packages and filters, install some filters to
        // some package.
        (if (pkgs > 0 && filts > 0) 50 else 0,
          for {
            pkg  <- Store.pickPackage.runA(s)
            filt <- Store.pickFilter.runA(s)
          } yield AddFilterToPackage(pkg, filt))

        // TODO generate components (add, edit, remove)

      ))
      _   <- StateT.stateTMonadState(monGen).set(semCommand(cmd).runS(s).run)
    } yield cmd
  // scalastyle:on

  def genCommands(n: Int)
                 (implicit ec: ExecutionContext): StateT[Gen, RawStore, List[Command]] =
    for {
      cmd  <- genCommand
      cmds <- if (n == 0) genCommand.map(List(_)) else genCommands(n - 1)
    } yield cmd :: cmds

}
