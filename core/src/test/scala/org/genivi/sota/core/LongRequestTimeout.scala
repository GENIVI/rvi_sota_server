/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.testkit.TestDuration
import scala.concurrent.duration._

trait LongRequestTimeout {
  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(10.seconds.dilated(system))
}
