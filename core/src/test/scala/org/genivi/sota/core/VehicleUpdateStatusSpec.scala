package org.genivi.sota.core

import org.genivi.sota.core.data.{UpdateStatus, VehicleStatus, VehicleSearch}
import org.genivi.sota.data.VehicleGenerators
import java.time.Instant
import org.scalatest.{FunSuite, ShouldMatchers}

class VehicleUpdateStatusSpec extends FunSuite
  with ShouldMatchers {

  import VehicleStatus._

  val vehicle = VehicleGenerators.genVehicle.sample.get.copy(lastSeen = Some(Instant.now))

  val now = Instant.now()

  test("Error if the last package is in Failed State") {
    val packages = List(now -> UpdateStatus.Failed, now.minusSeconds(60) -> UpdateStatus.Finished)
    val result = VehicleSearch.currentVehicleStatus(vehicle.lastSeen, packages)
    result shouldBe Error
  }

  test("out of date if any package is not finished") {
    val packages = List(now -> UpdateStatus.Pending, now -> UpdateStatus.InFlight)
    val result = VehicleSearch.currentVehicleStatus(vehicle.lastSeen, packages)
    result shouldBe Outdated
  }

  test("up to date if all packages are Finished or Error") {
    val packages = List(now -> UpdateStatus.Finished, now.minusSeconds(50) -> UpdateStatus.Failed)
    val result = VehicleSearch.currentVehicleStatus(vehicle.lastSeen, packages)
    result shouldBe UpToDate
  }

  test("not seen if vehicle was never seen") {
    val result = VehicleSearch.currentVehicleStatus(None, List.empty)
    result shouldBe NotSeen
  }
}
