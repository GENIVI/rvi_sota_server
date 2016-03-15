package org.genivi.sota.resolver.test

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.RouteTestTimeout
import cats.state.State
import io.circe.generic.auto._
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.packages.Package
import org.genivi.sota.resolver.test.random.Misc._
import org.genivi.sota.resolver.test.random.Semantics
import org.genivi.sota.resolver.test.random.Session, Session._
import org.genivi.sota.resolver.test.random.{Store, RawStore}
import org.genivi.sota.resolver.filters.Filter
import org.genivi.sota.rest.ErrorRepresentation
import scala.annotation.tailrec
import scala.concurrent.duration._


class Random extends ResourcePropSpec {
  // scalastyle:off
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
              case SuccessPackages(pkgs) => responseAs[Set[PackageId]]                    shouldBe pkgs
              case SuccessFilters(filts) => responseAs[Set[Filter]]                        shouldBe filts
              case SuccessVehicleMap(m)  => responseAs[Map[Vehicle.Vin, List[PackageId]]] shouldBe m
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

  implicit val config: PropertyCheckConfig =
    new PropertyCheckConfig(maxSize = 20, minSuccessful = 200)

  // scalastyle:on
  // We use a global variable to presist the state of the world between
  // the session runs.
  var s: RawStore = Store.initRawStore

  property("Sessions") {
    forAll { sesh: Session =>
      s = runSession(sesh).runS(s).run
      Store.validStore.isValid(s) shouldBe true
    }
  }

}
