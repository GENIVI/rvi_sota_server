package org.genivi.sota.resolver.test.random

import cats.state.{State, StateT}
import org.scalacheck.{Arbitrary, Gen, Shrink}
import Shrink.shrink
import Command._
import Misc._
import Query._
import Store._

import scala.concurrent.ExecutionContext


case class Session(commands: List[Command], queries: List[Query]) {

  def coverageInfo: SessionCoverage =
    SessionCoverage((commands ++ queries).map(_.getClass).toSet)

}

/**
  * Endpoints (encompassing commands and queries) that are exercised.
  */
case class SessionCoverage(covered: Set[Class[_]]) extends AnyVal {

  def merge(that: SessionCoverage): SessionCoverage =
    SessionCoverage(covered.union(that.covered))

  /**
    * Each position corresponds to an endpoint (command or query).
    * '1' indicates this session tests that endpoint, '0' otherwise.
    */
  override def toString: String = {
    val cs = Array.ofDim[Char](SessionCoverage.testSpaceSize)
    java.util.Arrays.fill(cs, '0')
    for (c <- covered) {
      val idx = SessionCoverage.testSpace.indexOf(c)
      cs(idx) = '1'
    }
    new String(cs)
  }

  def percent: Float = (covered.size / SessionCoverage.testSpaceSize.toFloat) * 100

  def untested: Set[String] =
    (SessionCoverage.testSpace.toSet -- covered) map { _.getSimpleName }

  def fullCoverage: Boolean = untested.isEmpty

}

object SessionCoverage {

  def testSpaceSize: Int = testSpace.length

  def emptyCoverageInfo: SessionCoverage = SessionCoverage(Set.empty)

  /**
    * Used to determine how much test coverage a Session achieves.
    * Alphabetically sorted to detect more easily missing commands.
    */
  val testSpace: Array[Class[_]] = Array(

    // commands
    classOf[AddComponent],
    classOf[AddFilter],
    classOf[AddFilterToPackage],
    classOf[AddPackage],
    classOf[AddVehicle],
    classOf[EditComponent],
    classOf[EditFilter],
    classOf[InstallComponent],
    classOf[InstallPackage],
    classOf[RemoveComponent],
    classOf[RemoveFilter],
    classOf[RemoveFilterForPackage],
    classOf[UninstallComponent],
    classOf[UninstallPackage],

    // queries
    ListVehicles.getClass,
    classOf[ListPackagesOnVehicle],
    classOf[ListVehiclesFor],
    classOf[ListPackagesFor],
    ListFilters.getClass,
    classOf[ListFiltersFor],
    classOf[Resolve],
    ListComponents.getClass,
    classOf[ListComponentsFor]
  )

}

object Session {

  def semSession(sesh: Session)
                (implicit ec: ExecutionContext): State[RawStore, List[Semantics]] =
    for {
      semCmds <- semCommands(sesh.commands)
      _ = println("\n\nCommands: " + sesh.commands.mkString("\n\t", "\n\t", "\n"))
      semQrys  <- semQueries(sesh.queries)
      qas = sesh.queries zip semQrys
      _ = qas foreach { case (q, a) =>
        println("Query: " + q)
        println("Result: " + a.result + "\n\n")
      }
    } yield (semCmds ++ semQrys)

  implicit def arbSession(implicit ec: ExecutionContext): Arbitrary[Session] =
    Arbitrary(genSession.runA(initRawStore))

  def genSession(implicit ec: ExecutionContext): StateT[Gen, RawStore, Session] =
    for {
      n1   <- lift(Gen.size)
      cmds <- genCommands(Math.max(1, n1))
      n2    = Math.max(1, n1/2)
      qrys <- genQueries(n2)
    } yield Session(cmds, qrys)

  implicit def shrinkSession: Shrink[Session] =
    Shrink { case Session(cmds, qrys) =>
      for {
        cmds2 <- shrink(cmds)
        qrys2 <- shrink(qrys)
      } yield Session(cmds2, qrys2)
    }

}
