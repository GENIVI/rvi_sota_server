/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.test

import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.RouteTestTimeout
import scala.concurrent.duration._
import akka.testkit.TestDuration

trait LongRequestTimeout {
  implicit def _default(implicit system: ActorSystem) = RouteTestTimeout(10.seconds.dilated(system))
}
