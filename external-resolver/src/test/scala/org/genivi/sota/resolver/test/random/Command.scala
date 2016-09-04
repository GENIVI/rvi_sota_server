package org.genivi.sota.resolver.test.random

import akka.http.scaladsl.model.{HttpRequest, StatusCode, StatusCodes}
import cats.state.{State, StateT}
import org.genivi.sota.data._
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.resolver.packages.{Package, PackageFilter}
import org.genivi.sota.resolver.test._
import org.genivi.sota.rest.ErrorCodes
import org.scalacheck.Gen

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import Misc._
import eu.timepit.refined.api.Refined
import org.genivi.sota.resolver.components.Component.PartNumber
import org.genivi.sota.resolver.test.generators.{ComponentGenerators, FilterGenerators, PackageGenerators}


// scalastyle:off number.of.types
sealed trait Command
trait WellFormedCommand extends Command
trait InvalidCommand    extends Command

/**
  * The good ones.
  */
final case class AddVehicle(device: Device) extends WellFormedCommand
final case class AddPackage(pkg: Package) extends WellFormedCommand

final case class InstallPackage  (veh: Device.Id, pkg: Package) extends WellFormedCommand
final case class UninstallPackage(veh: Device.Id, pkg: Package) extends WellFormedCommand

final case class AddFilter (filt: Filter)               extends WellFormedCommand
final case class EditFilter(old : Filter, neu: Filter)  extends WellFormedCommand {
  // restriction imposed by the endpoint allowing only non-PK fields to be updated.
  if (!(old.samePK(neu))) throw new IllegalArgumentException
}
final case class RemoveFilter          (filt: Filter)               extends WellFormedCommand
final case class AddFilterToPackage    (pkg: Package, filt: Filter) extends WellFormedCommand
final case class RemoveFilterForPackage(pkg: Package, filt: Filter) extends WellFormedCommand

final case class AddComponent   (cmpn: Component)                  extends WellFormedCommand
final case class EditComponent  (old : Component, neu: Component)  extends WellFormedCommand {
  // restriction imposed by the endpoint allowing only non-PK fields to be updated.
  if (!(old.samePK(neu))) throw new IllegalArgumentException
}
final case class RemoveComponent(cmpn: Component)                  extends WellFormedCommand
final case class InstallComponent  (veh: Device.Id, cmpn: Component) extends WellFormedCommand
final case class UninstallComponent(veh: Device.Id, cmpn: Component) extends WellFormedCommand

/**
  * The bad ones.
  */
final case class AddVehicleFail(device: Device.Id) extends InvalidCommand

final case class AddPackageFail(pkg: Package, statusCode: StatusCode, result: Result) extends InvalidCommand

final case class InstallPackageFail  (veh: Device.Id, pkg: Package) extends InvalidCommand
final case class UninstallPackageFail(veh: Device.Id, pkg: Package) extends InvalidCommand

final case class AddFilterFail (filt: Filter)               extends InvalidCommand
final case class EditFilterFail(old : Filter, neu: Filter)  extends InvalidCommand {
  // restriction imposed by the endpoint allowing only non-PK fields to be updated.
  if (!(old.samePK(neu))) throw new IllegalArgumentException
}
final case class RemoveFilterFail          (filt: Filter)               extends InvalidCommand
final case class AddFilterToPackageFail    (pkg: Package, filt: Filter) extends InvalidCommand
final case class RemoveFilterForPackageFail(pkg: Package, filt: Filter) extends InvalidCommand

final case class AddComponentFail   (cmpn: Component)                  extends InvalidCommand
final case class EditComponentFail  (old : Component, neu: Component)  extends InvalidCommand {
  // restriction imposed by the endpoint allowing only non-PK fields to be updated.
  if (!(old.samePK(neu))) throw new IllegalArgumentException
}
final case class RemoveComponentFail(cmpn: Component)                  extends InvalidCommand
final case class InstallComponentFail  (veh: Device.Id, cmpn: Component) extends InvalidCommand
final case class UninstallComponentFail(veh: Device.Id, cmpn: Component) extends InvalidCommand


trait CommandUtils extends
    VehicleRequestsHttp with
    PackageRequestsHttp with
    FilterRequestsHttp  with
    ComponentRequestsHttp with
    PackageFilterRequestsHttp with
    Namespaces {

  // scalastyle:off cyclomatic.complexity
  // scalastyle:off method.length
  /**
    * Command interpreter dealing with well-formed requests. A [[Semantics]] is prepared based on the given
    * command and the current state. Additionally, a new state may be prepared.
    */
  def semWellFormedCommand(cmd: WellFormedCommand)
                          (implicit ec: ExecutionContext): State[RawStore, Semantics] = cmd match {
    case AddVehicle(veh) =>
      for {
        s <- State.get
        _ <- State.set(s.creating(veh))
      } yield Semantics(addVehicle(veh.id), StatusCodes.OK, Success)

    case AddPackage(pkg) =>
      for {
        s <- State.get
        _ <- State.set(s.creating(pkg))
      } yield Semantics(addPackage(pkg), StatusCodes.OK, SuccessPackage(pkg))

    case InstallPackage(veh, pkg) =>
      for {
        s <- State.get
        _ <- State.set(s.installing(veh, pkg))
      } yield Semantics(installPackage(veh, pkg), StatusCodes.OK, Success)

    case UninstallPackage(veh, pkg) =>
      for {
        s <- State.get
        _ <- State.set(s.uninstalling(veh, pkg))
      } yield Semantics(
        uninstallPackage(veh, pkg),
        StatusCodes.OK, Success) // whether already uninstalled or not, OK is the reply

    case AddFilter(filt) =>
      for {
        s <- State.get
        _ <- State.set(s.creating(filt))
      } yield Semantics(addFilter2(filt), StatusCodes.OK, Success)

    case EditFilter(old, neu) =>
      for {
        s <- State.get
        _ <- State.set(s.replacing(old, neu))
      } yield Semantics(updateFilter(neu), StatusCodes.OK, Success)

    case RemoveFilter(filt) =>
      for {
        s       <- State.get
        _       <- State.set(s.removing(filt))
        success =  s.filtersUnused.contains(filt)
      } yield {
        val req = deleteFilter(filt)
        if (success) { Semantics(req, StatusCodes.OK, Success) }
        else         { Semantics(req, StatusCodes.Conflict, Failure(ErrorCodes.ConflictingEntity)) }
      }

    case AddFilterToPackage(pkg, filt) =>
      for {
        s       <- State.get
        _       <- State.set(s.associating(pkg, filt))
        success =  !s.packages(pkg).contains(filt)
      } yield {
        val req = addPackageFilter2(PackageFilter(pkg.namespace, pkg.id.name, pkg.id.version, filt.name))
        if (success) { Semantics(req, StatusCodes.OK, Success) }
        else         { Semantics(req, StatusCodes.Conflict, Failure(ErrorCodes.ConflictingEntity)) }
      }

    case RemoveFilterForPackage(pkg, filt) =>
      for {
        s       <- State.get
        _       <- State.set(s.deassociating(pkg, filt))
      } yield Semantics(
        deletePackageFilter(pkg, filt),
        StatusCodes.OK, Success)

    case AddComponent(cmpn) =>
      for {
        s <- State.get
        _ <- State.set(s.creating(cmpn))
        isDuplicatePK = s.components.exists(_.partNumber == cmpn.partNumber)
      } yield {
        val req = addComponent(cmpn.partNumber, cmpn.description)
        if (isDuplicatePK) { Semantics(req, StatusCodes.Conflict, Failure(ErrorCodes.ConflictingEntity)) }
        else               { Semantics(req, StatusCodes.OK, Success) }
      }

    case EditComponent(old, neu) =>
      for {
        s <- State.get
        _ <- State.set(s.replacing(old, neu))
      } yield Semantics(updateComponent(neu), StatusCodes.OK, Success)

    case RemoveComponent(cmpn) =>
      for {
        s <- State.get
        _ <- State.set(s.removing(cmpn))
        success = s.componentsUnused.contains(cmpn)
      } yield {
        val req = deleteComponent(cmpn.partNumber)
        if (success) { Semantics(req, StatusCodes.OK, Success) }
        else         { Semantics(req, StatusCodes.Conflict, Failure(ErrorCodes.ConflictingEntity)) }
      }

    case InstallComponent(veh, cmpn) =>
      for {
        s <- State.get
        _ <- State.set(s.installing(veh, cmpn))
        isDuplicatePK = s.devices(veh)._2.contains(cmpn)
      } yield {
        val req = installComponent(veh, cmpn)
        if (isDuplicatePK) { Semantics(req, StatusCodes.Conflict, Failure(ErrorCodes.ConflictingEntity)) }
        else               { Semantics(req, StatusCodes.OK, Success) }
      }

    case UninstallComponent(veh, cmpn) =>
      for {
        s <- State.get
        _ <- State.set(s.uninstalling(veh, cmpn))
      } yield Semantics(
        uninstallComponent(veh, cmpn),
        StatusCodes.OK, Success) // whether already uninstalled or not, OK is the reply

  }

  def genCommandAddVehicle: Gen[AddVehicle] =
    DeviceGenerators.genDevice.map(AddVehicle)

  // scalastyle:on
  def genCommandAddPackage: Gen[AddPackage] =
    PackageGenerators.genPackage.map(AddPackage)

  def genCommandAddFilter(s: RawStore): Gen[AddFilter] =
    FilterGenerators.genFilter(s.packages.keys.toList, s.components.toList)
      .map(AddFilter)

  def genCommandAddComponent(s: RawStore): Gen[AddComponent] =
    ComponentGenerators.genComponent.map(AddComponent)

  def genCommandInstallPackage(s: RawStore): Gen[InstallPackage] =
    for {
      veh <- Store.pickVehicle.runA(s)
      pkg <- Store.pickPackage.runA(s)
    } yield InstallPackage(veh, pkg)

  def genCommandUninstallPackage(s: RawStore): Gen[UninstallPackage] =
    for {
      (veh, pkg) <- Store.pickVehicleWithPackage.runA(s)
    } yield UninstallPackage(veh, pkg)

  def genCommandInstallComponent(s: RawStore): Gen[InstallComponent] =
    for {
      (veh, cmp) <- Store.pickVehicleComponentPairToInstall.runA(s)
    } yield InstallComponent(veh, cmp)

  def genCommandUninstallComponent(s: RawStore): Gen[UninstallComponent] =
    for {
      (veh, cmp) <- Store.pickVehicleWithComponent.runA(s)
    } yield UninstallComponent(veh, cmp)

  def genCommandAddFilterToPackage(s: RawStore): Gen[AddFilterToPackage] =
    for {
      (pkg, filt) <- Store.pickPackageFilterPairToInstall.runA(s)
    } yield AddFilterToPackage(pkg, filt)

  def genCommandRemoveFilterForPackage(s: RawStore): Gen[RemoveFilterForPackage] =
    for {
      (pkg, flt)  <- Store.pickPackageWithFilter.runA(s)
    } yield RemoveFilterForPackage(pkg, flt)

  def genCommandEditFilter(s: RawStore): Gen[EditFilter] =
    for {
      fltOld <- Store.pickFilter.runA(s)
      fltNu0 <- FilterGenerators.genFilter(s.packages.keys.toList, s.components.toList)
      fltNu1  = Filter(defaultNs, fltOld.name, fltNu0.expression)
    } yield EditFilter(fltOld, fltNu1)

  /**
    * Pick an unused filter for removal.
    */
  def genCommandRemoveFilter(s: RawStore): Gen[RemoveFilter] =
    for {
      flt  <- Store.pickUnusedFilter.runA(s)
    } yield RemoveFilter(flt)

  /**
    * Pick an unused component for removal.
    */
  def genCommandRemoveComponent(s: RawStore): Gen[RemoveComponent] =
    for {
      cmp  <- Store.pickUnusedComponent.runA(s)
    } yield RemoveComponent(cmp)

  def genCommandEditComponent(s: RawStore): Gen[EditComponent] =
    for {
      cmpOld <- Store.pickComponent.runA(s)
      cmpNu0 <- ComponentGenerators.genComponent
      cmpNu1  = Component(defaultNs, cmpOld.partNumber, cmpNu0.description)
    } yield EditComponent(cmpOld, cmpNu1)

}

trait InvalidCommandUtils { _: CommandUtils =>

  import InvalidDeviceGenerators._
  import org.genivi.sota.resolver.test.generators.InvalidFilterGenerators._
  import org.genivi.sota.resolver.test.generators.InvalidComponentGenerators._

  def getIdent: String = Gen.identifier.sample.getOrElse(getIdent)

  def emptyPackageIdName:    PackageId.Name    = Refined.unsafeApply("")
  def emptyPackageIdVersion: PackageId.Version = Refined.unsafeApply("")

  def pickOneOf[T](g: Gen[T]): T = g.sample.getOrElse(pickOneOf(g))

  def pickOneOf[T](t1: T, t2: T, tn: T*): T = pickOneOf(Gen.oneOf(t1, t2, tn: _*))

  def getBadReqAddPackage(pkg: Package, statusCode: StatusCode, result: Result)
                         (implicit ec: ExecutionContext): Semantics =
    Semantics(addPackage(pkg), statusCode, result)

  def getBadReqInstallPackage(veh: Device.Id, pkg: Package)
                             (implicit ec: ExecutionContext): Semantics =
    Semantics(installPackage(veh, pkg), StatusCodes.BadRequest, Success)

  def getBadReqUninstallPackage(veh: Device.Id, pkg: Package)
                               (implicit ec: ExecutionContext): Semantics =
    Semantics(uninstallPackage(veh, pkg), StatusCodes.BadRequest, Success)

  def getBadReqAddFilter(flt: Filter)
                        (implicit ec: ExecutionContext): Semantics =
    Semantics(addFilter2(flt), StatusCodes.BadRequest, Success)

  def getBadReqEditFilter(old: Filter, neu: Filter)
                         (implicit ec: ExecutionContext): Semantics =
    Semantics(updateFilter(neu), StatusCodes.BadRequest, Success)

  def getBadReqRemoveFilter(flt: Filter)
                           (implicit ec: ExecutionContext): Semantics = {
    val req = deleteFilter(flt)
    Semantics(req, StatusCodes.BadRequest, Failure(ErrorCodes.InvalidEntity))
  }

  def getBadReqAddFilterToPackage(pkg: Package, flt: Filter)
                                 (implicit ec: ExecutionContext): Semantics = {
    val pkf = PackageFilter(pkg.namespace, pkg.id.name, pkg.id.version, flt.name)
    val req = addPackageFilter2(pkf)
    Semantics(req, StatusCodes.BadRequest, Success)
  }

  def getBadReqRemoveFilterForPackage(pkg: Package, flt: Filter)
                                     (implicit ec: ExecutionContext): Semantics = {
    val req = deletePackageFilter(pkg, flt)
    Semantics(req, StatusCodes.BadRequest, Success)
  }

  def getBadReqAddComponent(cmpn: Component)
                           (implicit ec: ExecutionContext): Semantics = {
    val req = addComponent(cmpn)
    Semantics(req, StatusCodes.BadRequest, Success)
  }

  def getBadReqEditComponent(old: Component, neu: Component)
                            (implicit ec: ExecutionContext): Semantics = {
    val req = updateComponent(neu)
    Semantics(req, StatusCodes.BadRequest, Success)
  }

  def getBadReqRemoveComponent(cmpn: Component)
                              (implicit ec: ExecutionContext): Semantics = {
    val req = deleteComponent(cmpn)
    Semantics(req, StatusCodes.BadRequest, Success)
  }

  def getBadReqInstallComponent(veh: Device.Id, cmpn: Component)
                               (implicit ec: ExecutionContext): Semantics = {
    val req = installComponent(veh, cmpn)
    Semantics(req, StatusCodes.BadRequest, Success)
  }

  def getBadReqUninstallComponent(veh: Device.Id, cmpn: Component)
                                 (implicit ec: ExecutionContext): Semantics = {
    val req = uninstallComponent(veh, cmpn)
    Semantics(req, StatusCodes.BadRequest, Success)
  }

  import org.genivi.sota.resolver.test.generators.InvalidPackageGenerators._
  def xlateToInvalidCommand(cmd: WellFormedCommand): InvalidCommand = cmd match {
    case AddVehicle(veh) =>
      AddVehicleFail(veh.id)

    case AddPackage(pkg) =>
      def expectNotFound(pkg: Package) =
        AddPackageFail(pkg, StatusCodes.NotFound, Success)

      def expectBadRequest(pkg: Package) =
        AddPackageFail(pkg, StatusCodes.BadRequest, Failure(ErrorCodes.InvalidEntity))

      pickOneOf(
        // not accept empty package name
        expectNotFound(pkg.copy(id = pkg.id.copy(name = emptyPackageIdName))),

        // not accept invalid package name
        expectBadRequest(pkg.copy(id = pkg.id.copy(name = pickOneOf(genInvalidPackageIdName)))),

        // not accept empty package version
        expectNotFound(pkg.copy(id = pkg.id.copy(version = emptyPackageIdVersion))),

        // not accept invalid package versions
        expectBadRequest(pkg.copy(id = pkg.id.copy(version = pickOneOf(genInvalidPackageIdVersion))))
      )

    case InstallPackage(veh0, pkg0) =>
      val (veh: Device.Id, pkg: Package) = pickOneOf(
        (veh0, getInvalidPackage)
      )
      InstallPackageFail(veh, pkg)

    case UninstallPackage(veh0, pkg0) =>
      val (veh: Device.Id, pkg: Package) = pickOneOf(
        (veh0, getInvalidPackage)
      )
      UninstallPackageFail(veh, pkg)

    case AddFilter(filt) =>
      AddFilterFail(getInvalidFilter)

    case EditFilter(_, _) =>
      val flt = getInvalidFilter
      EditFilterFail(flt, flt)

    case RemoveFilter(_) =>
      // fail on trying to delete non-existing filters
      RemoveFilterFail(getInvalidFilter)

    case AddFilterToPackage(pkg0, flt0) =>
      val pkg = pickOneOf(
        pkg0.copy(id = pkg0.id.copy(name = getInvalidPackageIdName)),
        pkg0.copy(id = pkg0.id.copy(version = getInvalidPackageIdVersion))
      )
      val flt = flt0.copy(name = getInvalidFilterName)
      AddFilterToPackageFail(pkg, flt)

    case RemoveFilterForPackage(pkg0, flt0) =>
      val (pkg: Package, flt: Filter) = pickOneOf(
        (pkg0, getInvalidFilter),
        (getInvalidPackage, flt0)
      )
      RemoveFilterForPackageFail(pkg, flt)

    case AddComponent(_) =>
      AddComponentFail(getInvalidComponent)

    case EditComponent(_, _) =>
      val cmpn = getInvalidComponent
      EditComponentFail(cmpn, cmpn)

    case RemoveComponent(_) =>
      RemoveComponentFail(getInvalidComponent)

    case InstallComponent(veh0, cmpn0) =>
      val (veh: Device.Id, cmpn: Component) = pickOneOf(
        (veh0, getInvalidComponent)
      )
      InstallComponentFail(veh, cmpn)

    case UninstallComponent(veh0, cmpn0) =>
      val (veh: Device.Id, cmpn: Component) = pickOneOf(
        (veh0, getInvalidComponent)
      )
      UninstallComponentFail(veh, cmpn)

  }

}

object Command extends CommandUtils with InvalidCommandUtils {

  type SemCommand = (HttpRequest, StatusCode, Result)

  def semCommands(cmds: List[Command])
                 (implicit ec: ExecutionContext): State[RawStore, List[Semantics]] = {

    @tailrec def go(cmds0: List[Command], s0: RawStore, acc: List[Semantics]): (RawStore, List[Semantics]) =
      cmds0 match {
        case Nil            => (s0, acc.reverse)
        case (cmd :: cmds1) =>
          val (s1, r) = semCommand(cmd).run(s0).run
          go(cmds1, s1, r :: acc)
      }

    State.get.flatMap { s0 =>
      val (s1, sems) = go(cmds, s0, List())
      State.set(s1).flatMap(_ => State.pure(sems))
    }

  }

  /**
    * Command interpreter preparing a [[Semantics]] based on the command and the current state.
    */
  def semCommand(cmd: Command)
                (implicit ec: ExecutionContext): State[RawStore, Semantics] = cmd match {
    case wfc: WellFormedCommand => semWellFormedCommand(wfc)
    case ic:  InvalidCommand    => semInvalidCommand(ic)
  }

  def genCommands(n: Int)
                 (implicit ec: ExecutionContext): StateT[Gen, RawStore, List[Command]] = {
    if (n < 1) throw new IllegalArgumentException
    for {
      cmd  <- genCommand
      cmds <- if (n == 1) genCommand.map(List(_)) else genCommands(n - 1)
    } yield cmd :: cmds
  }

  // scalastyle:off cyclomatic.complexity
  // scalastyle:off magic.number
  def genCommand(implicit ec: ExecutionContext): StateT[Gen, RawStore, Command] =
    for {
      s     <- StateT.stateTMonadState[Gen, RawStore].get
      vehs  <- Store.numberOfVehicles
      pkgs  <- Store.numberOfPackages
      filts <- Store.numberOfFilters
      uflts <- Store.numberOfUnusedFilters
      comps <- Store.numberOfComponents
      ucmps <- Store.numberOfUnusedComponents
      vcomp <- Store.numberOfVehiclesWithSomeComponent
      vpaks <- Store.numberOfVehiclesWithSomePackage
      pfilt <- Store.numberOfPackagesWithSomeFilter
      vcnst <- Store.numberOfVehicleComponentPairsToInstall
      pfnst <- Store.numberOfPackageFilterPairsToInstall
      cmd0  <- lift(Gen.frequency(

        // If there are few vehicles, packages or filters in the world,
        // then generate some with high probability.

        (if (vehs <= 10) 100 else 1, genCommandAddVehicle),

        (if (pkgs <= 5)  100 else 1, genCommandAddPackage),

        (if (filts <= 3) 20 else 1, genCommandAddFilter(s)),

        (if (comps <= 3) 20 else 1, genCommandAddComponent(s)),

        // If there are vehicles and packages, then install some
        // packages on the vehicles with high probability.
        (if (vehs > 0 && pkgs > 0) 100 else 0, genCommandInstallPackage(s)),

        // If there are components that can be installed on vehicles
        (if (vcnst > 0) 100 else 0, genCommandInstallComponent(s)),

        (if (vpaks > 0) 10 else 0, genCommandUninstallPackage(s)),

        (if (vcomp > 0) 10 else 0, genCommandUninstallComponent(s)),

        (if (filts > 0) 50 else 0, genCommandEditFilter(s)),

        (if (comps > 0) 50 else 0, genCommandEditComponent(s)),

        // If there are packages and filters, install some filter to some package.
        (if (pfnst > 0) 50 else 0, genCommandAddFilterToPackage(s)),

        (if (pfilt > 0) 50 else 0, genCommandRemoveFilterForPackage(s)),

        (if (uflts > 0) 50 else 0, genCommandRemoveFilter(s)),

        (if (ucmps > 0) 50 else 0, genCommandRemoveComponent(s))

      ))

      cmd <- lift(Gen.frequency(
        (50, cmd0),
        (50, xlateToInvalidCommand(cmd0))
      ))

      _   <- StateT.stateTMonadState(monGen).set(semCommand(cmd).runS(s).run)
    } yield cmd
  // scalastyle:on

  /**
    * Command interpreter dealing with bad requests. For each [[InvalidCommand]] one [[Semantics]] is prepared
    * while maintaining state.
    */
  def semInvalidCommand(cmd: InvalidCommand)
                       (implicit ec: ExecutionContext): State[RawStore, Semantics] = {
    val sem = cmd match {
      case AddVehicleFail(device: Device.Id) =>
        import akka.http.scaladsl.client.RequestBuilding.Get
        Semantics(Get("/fake_devices"), StatusCodes.MethodNotAllowed, Success)

      case AddPackageFail(pkg, statusCode, result) => getBadReqAddPackage(pkg, statusCode, result)

      case InstallPackageFail(veh, pkg) => getBadReqInstallPackage(veh, pkg)

      case UninstallPackageFail(veh, pkg) => getBadReqUninstallPackage(veh, pkg)

      case AddFilterFail(flt) => getBadReqAddFilter(flt)

      case EditFilterFail(old, neu) => getBadReqEditFilter(old, neu)

      case RemoveFilterFail(flt) => getBadReqRemoveFilter(flt)

      case AddFilterToPackageFail(pkg, flt) => getBadReqAddFilterToPackage(pkg, flt)

      case RemoveFilterForPackageFail(pkg, flt) => getBadReqRemoveFilterForPackage(pkg, flt)

      case AddComponentFail(cmpn) => getBadReqAddComponent(cmpn)

      case EditComponentFail(old, neu) => getBadReqEditComponent(old, neu)

      case RemoveComponentFail(cmpn) => getBadReqRemoveComponent(cmpn)

      case InstallComponentFail(veh, cmpn) => getBadReqInstallComponent(veh, cmpn)

      case UninstallComponentFail(veh, cmpn) => getBadReqUninstallComponent(veh, cmpn)

    }

    State.get map { s => sem }
  }

}
// scalastyle:on
