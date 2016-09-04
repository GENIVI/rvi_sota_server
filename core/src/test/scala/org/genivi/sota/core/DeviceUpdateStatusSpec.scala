package org.genivi.sota.core

import java.time.Instant

import org.genivi.sota.core.data.{DeviceSearch, DeviceStatus, UpdateStatus}
import org.scalatest.{FunSuite, ShouldMatchers}

class DeviceUpdateStatusSpec extends FunSuite
  with ShouldMatchers {

  import DeviceStatus._
  import org.genivi.sota.data.DeviceGenerators._

  val device = genDevice.sample.get.copy(lastSeen = Some(Instant.now))

  val now = Instant.now()

  test("Error if the last package is in Failed State") {
    val packages = List(now -> UpdateStatus.Failed, now.minusSeconds(10) -> UpdateStatus.Finished)
    val result = DeviceSearch.currentDeviceStatus(device.lastSeen, packages)
    result shouldBe Error
  }

  test("up to date if there are no specs and vehicle was seen") {
    val result = DeviceSearch.currentDeviceStatus(device.lastSeen, List.empty)
    result shouldBe UpToDate
  }

  test("out of date if any package is not finished") {
    val packages = List(now -> UpdateStatus.Pending, now -> UpdateStatus.InFlight)
    val result = DeviceSearch.currentDeviceStatus(device.lastSeen, packages)
    result shouldBe Outdated
  }

  test("up to date even if some packages are cancelled") {
    val packages = List(now -> UpdateStatus.Finished, now.minusSeconds(50) -> UpdateStatus.Canceled)
    val result = DeviceSearch.currentDeviceStatus(device.lastSeen, packages)
    result shouldBe UpToDate
  }

  test("up to date if all packages are Finished or Error") {
    val packages = List(now -> UpdateStatus.Finished, now.minusSeconds(50) -> UpdateStatus.Failed)
    val result = DeviceSearch.currentDeviceStatus(device.lastSeen, packages)
    result shouldBe UpToDate
  }

  test("not seen if device was never seen") {
    val result = DeviceSearch.currentDeviceStatus(None, List.empty)
    result shouldBe NotSeen
  }
}
