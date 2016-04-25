package org.genivi.sota.resolver.test

import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.RouteTestTimeout
import cats.state.State
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.test.random.Misc._
import org.genivi.sota.resolver.test.random._
import Session._
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.rest.ErrorRepresentation

import scala.annotation.tailrec
import scala.concurrent.duration._


/**
  *  Regarding test coverage: This test runs until each REST endpoint has been exercised at least once.
  *  An alternative design (reporting test coverage as a percentage) was also explored:
  *  <ul>
  *    <li>The Scalatest way to summarize information for a test involves writing a custom [[org.scalatest.Reporter]]
  *        receiving events </li>
  *    <li>For example, [[org.scalatest.events.TestSucceeded]] events with a custom payload
  *        (for example, which commands and queries the test in question exercised).</li>
  *  </ul>
  */
class Random extends ResourcePropSpec {

  val config = system.settings.config

  // scalastyle:off cyclomatic.complexity
  def runSession(sesh: Session)(implicit route: Route): State[RawStore, Unit] = {

    @tailrec def go(sems: List[Semantics]): Unit =
      sems match {
        case Nil           => ()
        case (sem :: sems) =>

          implicit val routeTimeout: RouteTestTimeout =
            RouteTestTimeout(10.second)

          sem.request ~> route ~> check {
            status shouldBe sem.statusCode
            sem.result match {
              case Failure(c)            => responseAs[ErrorRepresentation].code           shouldBe c
              case Success               => ()
              case SuccessVehicles(vehs) => responseAs[Set[Vehicle]]                       shouldBe vehs
              case SuccessPackage(pkg)   => responseAs[Package]                            shouldBe pkg
              case SuccessPackages(pkgs) => responseAs[Set[Package]]                       shouldBe pkgs
              case SuccessPackageIds(pids) => responseAs[Set[PackageId]]                   shouldBe pids
              case SuccessFilters(filts) => responseAs[Set[Filter]]                        shouldBe filts
              case SuccessComponents(cs) => responseAs[Set[Component]]                     shouldBe cs
              case SuccessPartNumbers(x) => responseAs[Set[Component.PartNumber]]          shouldBe x
              case SuccessVehicleMap(m)  => responseAs[Map[Vehicle.Vin, List[PackageId]]]  shouldBe m
              case r                     => sys.error(s"runSession: non-exhaustive pattern: $r")
            }
          }
          go(sems)
      }

    State.modify { s0 =>
      val (s1, sems)  = semSession(sesh).run(s0).run
      val _           = go(sems)
      s1
    }

  }
  // scalastyle:on

  implicit val propCheckConfig: PropertyCheckConfig =
    new PropertyCheckConfig(
      maxSize = 20, // scalastyle:ignore magic.number
      minSuccessful = config.getInt("test.random.minSuccessful"))

  // We use a global variable to persist the state of the world between the session runs.
  var s: RawStore = Store.initRawStore

  // accumulates test-coverage across all session
  var accCoverage = SessionCoverage.emptyCoverageInfo

  property("Sessions") {
    do {
      forAll { sesh: Session =>
        accCoverage = accCoverage.merge(sesh.coverageInfo)
        s = runSession(sesh).runS(s).run
        Store.validStore.isValid(s) shouldBe true
      }
    } while (!accCoverage.fullCoverage)
    info("Coverage:")
    accCoverage.prettyPrint.foreach(info(_))
  }

}
