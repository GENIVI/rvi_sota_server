package org.genivi.sota.resolver.test.random

import cats.state.{State, StateT}
import org.scalacheck.{Arbitrary, Gen, Shrink}
import Shrink.shrink
import Command._
import Misc._
import Query._
import Store._
import scala.language.existentials

import scala.concurrent.ExecutionContext


case class Session(commands: List[Command], queries: List[Query]) {

  def coverageInfo: SessionCoverage =
    SessionCoverage(
      (commands ++ queries).groupBy(_.getClass).map { case (klazz, cmds) => (klazz, cmds.size) }.toMap
    )

}

/**
  * Coverage as measured before interpretation. Basically:
  * <ul>
  *   <li>which endpoints (for commands and queries) are exercised</li>
  *   <li>how many times</li>
  * </ul>
  * Stats about query sizes are gathered after interpretation, see [[QueryKindAggregates]].
  */
case class SessionCoverage(occurrences: Map[Class[_], Int]) {

  def covered: Set[Class[_]] = occurrences.keySet

  def merge(that: SessionCoverage): SessionCoverage = SessionCoverage(
    mergeAddingIntValues(occurrences, that.occurrences)
  )

  private def mergeAddingIntValues(map1: Map[Class[_], Int],
                                   map2: Map[Class[_], Int]): Map[Class[_], Int] =
    map1 ++ map2.map { case (k,v) => k -> (v + map1.getOrElse(k,0)) }

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

  /**
    * As defined here, full-coverage doesn't take into account the cardinality of query resultsets.
    */
  def fullCoverage: Boolean = untested.isEmpty

  def occurrencesWellFormedCommands: Map[Class[_], Int] =
    occurrences filter { case (klazz, _) => classOf[WellFormedCommand].isAssignableFrom(klazz) }

  def occurrencesInvalidCommands: Map[Class[_], Int] =
    occurrences filter { case (klazz, _) => classOf[InvalidCommand].isAssignableFrom(klazz) }

  def occurrencesQueries: Map[Class[_], Int] =
    occurrences filter { case (klazz, _) => classOf[Query].isAssignableFrom(klazz) }

  /**
   * For each entry (kind of command or query, number of occurrences) a line is produced,
   * listing the fraction it represents of all entries. For a query kind, additionally the average
   * cardinality over its resultsets is also included.
   */
  def prettyPrint(qryAggregates: QueryKindAggregates): Seq[String] = {

    import SessionCoverage.{Summary, SummaryLine}

      def toSummary(occs    : Map[Class[_], Int],
                    qryStats: Map[Class[_], Double]): Summary = {
        val totalOccs = occs.foldLeft(0) { (acc, entry) => val (klazz, num) = entry; acc + num }
        val lines = occs.map({ case (klazz, num) =>
          SummaryLine(klazz.getSimpleName, num, qryStats.get(klazz))
        }).toSeq
        Summary(lines, totalOccs)
      }

      def pp(s: Summary): Seq[String] = {
        s.lines.sortBy(_.label).map(_.format(s.totalOccs))
      }

    ("Well-formed commands (100%)" +: pp(toSummary(occurrencesWellFormedCommands, Map.empty))) ++
    ("Invalid commands (100%)"     +: pp(toSummary(occurrencesInvalidCommands,    Map.empty))) ++
    ("Queries (100%)"  +: pp(toSummary(occurrencesQueries,  qryAggregates.avgSizePerQryKind )))
  }

}

/**
  * For a given [[Query]] kind (not shown) the running total of its resultset sizes, and the number of such queries.
  */
case class QueryKindAggregate(cardinality: Int, numberOfQueries: Int) {
  def merge(that: QueryKindAggregate): QueryKindAggregate = QueryKindAggregate(
    cardinality + that.cardinality, numberOfQueries + that.numberOfQueries
  )
}

/**
  * Key: [[Query]] kind (represented by the class for that query).
  */
case class QueryKindAggregates(qkas: Map[Class[_], QueryKindAggregate]) extends AnyVal {

  def merge(that: QueryKindAggregates): QueryKindAggregates = QueryKindAggregates(
    qkas ++ that.qkas.map { case (k,v2) =>
      val v1 = qkas.getOrElse(k, QueryKindAggregate(0, 0))
      k -> (v1.merge(v2))
    }
  )

  /**
    * For each query-kind, the average cardinality of its resultsets.
    */
  def avgSizePerQryKind: Map[Class[_], Double] = qkas mapValues {
    case QueryKindAggregate(cardinality, numberOfQueries) =>
      cardinality / numberOfQueries.toDouble
  }

}

object SessionCoverage {

  /**
    * <ul>
    * <li>Label denotes the kind of [[Command]] or [[Query]]</li>
    * <li>Num the number of occurrences for that kind</li>
    * <li>The last argument is non-empty only for queries, the average cardinality over all results sets for that
    * kind of query.</li>
    * </ul>
    */
  case class SummaryLine(label: String,
                         num: Int,
                         avgQrySize: Option[Double]) {
    def format(totalOccs: Int): String = {

        def fmtS(d: Double) = f"${d}%5.1f"
        def fmtL(d: Double) = f"${d}%10.1f"

      val percent = (num * 100) / totalOccs.toDouble
      val fmtNum = f"$num%4d"
      val fmtPercent = s"${fmtS(percent)}%"
      val fmtQrySize = avgQrySize match {
        case None => ""
        case Some(avg) => s"Avg cardinality: ${fmtL(avg)}"
      }
      f"\t$label%30s: $fmtNum%6s ($fmtPercent%5s) $fmtQrySize%10s"
    }
  }

  /**
    * A [[Summary]] for either the set of commands or the set of queries,
    * with one line for each command or query. The second argument
    * counts all occurrences in the summary (ie, all commands being summarized, or all queries being summarized).
    */
  case class Summary(lines: Seq[SummaryLine], totalOccs: Int)

  def testSpaceSize: Int = testSpace.length

  def emptyCoverageInfo: SessionCoverage = SessionCoverage(Map.empty)

  /**
    * Used to determine how much test coverage a Session achieves.
    * Alphabetically sorted to detect more easily missing commands.
    */
  val testSpace: Array[Class[_]] = Array(

    // well-formed commands
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

    // invalid commands
    classOf[AddComponentFail],
    classOf[AddFilterFail],
    classOf[AddFilterToPackageFail],
    classOf[AddPackageFail],
    classOf[AddVehicleFail],
    classOf[EditComponentFail],
    classOf[EditFilterFail],
    classOf[InstallComponentFail],
    classOf[InstallPackageFail],
    classOf[RemoveComponentFail],
    classOf[RemoveFilterFail],
    classOf[RemoveFilterForPackageFail],
    classOf[UninstallComponentFail],
    classOf[UninstallPackageFail],

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
      n2    = Math.max(1, n1/4)
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
