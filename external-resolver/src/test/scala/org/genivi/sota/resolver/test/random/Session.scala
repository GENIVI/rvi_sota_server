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
    SessionCoverage(
      (commands ++ queries).groupBy(_.getClass).map { case (klazz, cmds) => (klazz, cmds.size) }.toMap
    )

}

/**
  * Endpoints (encompassing commands and queries) that are exercised.
  */
case class SessionCoverage(occurrences: Map[Class[_], Int]) extends AnyVal {

  def covered: Set[Class[_]] = occurrences.keySet

  def merge(that: SessionCoverage): SessionCoverage = {
    SessionCoverage(
      occurrences ++ that.occurrences.map { case (k,v) => k -> (v + occurrences.getOrElse(k,0)) }
    )
  }

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

  def occurrencesCommands: Map[Class[_], Int] =
    occurrences filter { case (klazz, _) => classOf[Command].isAssignableFrom(klazz) }

  def occurrencesQueries: Map[Class[_], Int] =
    occurrences filter { case (klazz, _) => classOf[Query].isAssignableFrom(klazz) }

  /**
   * For each entry (kind of command or query, number of occurrences) a line is produced,
   * listing along the above a percentage with respect to all entries.
   */
  def prettyPrint: Seq[String] = {

    import SessionCoverage.{Summary, SummaryLine}

      def toSummary(occs: Map[Class[_], Int]): Summary = {
        val totalOccs = occs.foldLeft(0) { (acc, entry) => val (klazz, num) = entry; acc + num }
        val lines = occs.map({ case (klazz, num) => SummaryLine(klazz.getSimpleName, num) }).toSeq
        Summary(lines, totalOccs)
      }

      def pp(s: Summary): Seq[String] = {
        s.lines.sortBy(_.label).map(_.format(s.totalOccs))
      }

    ("Commands" +: pp(toSummary(occurrencesCommands))) ++ ("Queries" +: pp(toSummary(occurrencesQueries)))
  }

}

object SessionCoverage {

  case class SummaryLine(label: String, num: Int) {
    def format(totalOccs: Int): String = {
      val percent = (num * 100) / totalOccs.toDouble
      val fmtPercent = f"${percent}%2.1f%%"
      f"\t$label%25s:\t $num%4d ($fmtPercent%5s)"
    }
  }

  case class Summary(lines: Seq[SummaryLine], totalOccs: Int)

  def testSpaceSize: Int = testSpace.length

  def emptyCoverageInfo: SessionCoverage = SessionCoverage(Map.empty)

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
