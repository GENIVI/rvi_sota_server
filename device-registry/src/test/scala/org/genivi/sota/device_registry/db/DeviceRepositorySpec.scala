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
      uuid <- DeviceRepository.create(Namespaces.defaultNs, device)
      first <- DeviceRepository.updateLastSeen(uuid, Instant.now()).map(_._1)
      second <- DeviceRepository.updateLastSeen(uuid, Instant.now()).map(_._1)
    } yield (first, second)

    whenReady(db.run(setTwice), Timeout(Span(10, Seconds))) {
      case (f, s) => f shouldBe(true)
                     s shouldBe(false)
    }
  }

  test("activated_at can be counted") {

    val device = genDeviceT.sample.get.copy(deviceId = Some(genDeviceId.sample.get))
    val createDevice = for {
      uuid <- DeviceRepository.create(Namespaces.defaultNs, device)
      now = Instant.now()
      _ <- DeviceRepository.updateLastSeen(uuid, now)
      count <- DeviceRepository.countActivatedDevices(Namespaces.defaultNs, now, now.plusSeconds(100))
    } yield count

    whenReady(db.run(createDevice), Timeout(Span(10, Seconds))) { count =>
      count shouldBe(1)
    }
  }
}
