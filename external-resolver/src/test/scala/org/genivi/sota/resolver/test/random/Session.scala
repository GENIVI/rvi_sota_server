package org.genivi.sota.resolver.test.random

import cats.state.{State, StateT}
import org.scalacheck.{Arbitrary, Gen, Shrink}
import Shrink.shrink
import Command._
import Misc._
import Query._
import Store._

import scala.concurrent.ExecutionContext


case class Session(commands: List[Command], query: Query)

object Session {

  def semSession(sesh: Session)
                (implicit ec: ExecutionContext): State[RawStore, List[Semantics]] =
    for {
      semCmds <- semCommands(sesh.commands)
      _ = println("\n\nCommands: " + sesh.commands.mkString("\n\t", "\n\t", "\n"))
      semQry  <- semQuery(sesh.query)
      _ = println("Query: " + sesh.query)
      _ = println("Result: " + semQry.result + "\n\n")
    } yield (semCmds :+ semQry)

  implicit def arbSession(implicit ec: ExecutionContext): Arbitrary[Session] =
    Arbitrary(genSession.runA(initRawStore))

  def genSession(implicit ec: ExecutionContext): StateT[Gen, RawStore, Session] =
    for {
      n    <- lift(Gen.size)
      cmds <- genCommands(n)
      qry  <- genQuery
    } yield Session(cmds, qry)

  implicit def shrinkSession: Shrink[Session] =
    Shrink { case Session(cmds, qry) =>
      for { cmds2 <- shrink(cmds) } yield Session(cmds2, qry)
    }

}
