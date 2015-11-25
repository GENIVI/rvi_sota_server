package org.genivi.sota.resolver.test.random

import cats.state.{State, StateT}
import org.scalacheck.{Arbitrary, Gen}
import Command._
import Misc._
import Query._
import Store._


case class Session(commands: List[Command], query: Query)

object Session {

  def semSession(sesh: Session): State[RawStore, List[Semantics]] =
    for {
      semCmds <- semCommands(sesh.commands)
      _ = println("\n\nCommands: " + sesh.commands)
      semQry  <- semQuery(sesh.query)
      _ = println("Query: " + sesh.query)
      _ = println("Result: " + semQry.result + "\n\n")
    } yield (semCmds :+ semQry)

  implicit def arbSession: Arbitrary[Session] =
    Arbitrary(genSession.runA(initRawStore))

  def genSession: StateT[Gen, RawStore, Session] =
    for {
      n    <- lift(Gen.size)
      cmds <- genCommands(n)
      qry  <- genQuery
    } yield Session(cmds, qry)

}
