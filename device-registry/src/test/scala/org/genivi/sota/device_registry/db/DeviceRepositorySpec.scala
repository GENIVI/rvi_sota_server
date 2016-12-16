/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */

package org.genivi.sota.device_registry.db

import java.time.Instant

import org.genivi.sota.core.DatabaseSpec
import org.genivi.sota.data.DeviceGenerators.{genDeviceId, genDeviceT}
import org.genivi.sota.data.Namespaces
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FunSuite, ShouldMatchers}

import scala.concurrent.ExecutionContext.Implicits.global

class DeviceRepositorySpec extends FunSuite
  with DatabaseSpec
  with ScalaFutures
  with ShouldMatchers {

  test("updateLastSeen sets activated_at the first time only") {

    val device = genDeviceT.sample.get.copy(deviceId = Some(genDeviceId.sample.get))
    val setTwice = for {
      uuid <- db.run(DeviceRepository.create(Namespaces.defaultNs, device))
      first <- db.run(DeviceRepository.updateLastSeen(uuid, Instant.now()))
      second <- db.run(DeviceRepository.updateLastSeen(uuid, Instant.now()))
    } yield (first, second)

    whenReady(setTwice, Timeout(Span(10, Seconds))) {
      case (f, s) => f shouldBe(true)
                     s shouldBe(false)
    }
  }
}
