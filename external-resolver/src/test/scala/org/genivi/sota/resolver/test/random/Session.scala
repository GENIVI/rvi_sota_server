package org.genivi.sota.resolver.test.random

import cats.state.{State, StateT}
import org.scalacheck.{Arbitrary, Gen, Shrink}
import Shrink.shrink
import Command._
import Misc._
import Query._
import Store._

import scala.concurrent.ExecutionContext


case class Session(commands: List[Command], queries: List[Query])

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
    Shrink { case Session(cmds, qry) =>
      for { cmds2 <- shrink(cmds) } yield Session(cmds2, qry)
    }

}
