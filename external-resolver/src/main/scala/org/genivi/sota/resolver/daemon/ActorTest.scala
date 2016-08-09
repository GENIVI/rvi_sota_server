/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.daemon

import akka.actor.{Actor, ActorSystem, Props}
import akka.actor.Actor.Receive
import org.genivi.sota.resolver.daemon.Messages.{Die, Start, Tick}
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

object  Messages {
  case object Start
  case object Die
  case object Tick
}

class Slave extends Actor {
  override def receive: Receive = {
    case Die =>
      println("Will stop myself")
      context.stop(self)
  }
}

class Master extends Actor {
  implicit val exec = context.dispatcher

  context.system.scheduler.schedule(5.seconds, 5.seconds, self, Tick)

  override def receive: Receive = {
    case Tick =>
      println("Still ticking")

    case Start =>
      val ref = context.system.actorOf(Props[Slave])
      println("Will send Die")
      ref ! Die

      context.system.scheduler.scheduleOnce(5.seconds, ref, Tick)

    case wat =>
      println("Wat now? " + wat)
  }
}

object ActorTest extends App {

  implicit val system = ActorSystem("sota-resolver-bus-listener")
  implicit val exec = system.dispatcher
  implicit val log = LoggerFactory.getLogger(this.getClass)

  system.actorOf(Props[Master]) ! Start
}
