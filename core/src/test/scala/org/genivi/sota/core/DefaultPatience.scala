/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core

import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Millis, Seconds, Span}

trait DefaultPatience {
  self: PatienceConfiguration =>

  override implicit def patienceConfig = PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))
}
