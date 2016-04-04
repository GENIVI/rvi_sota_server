package org.genivi.sota.core

import org.genivi.sota.core.data.{UpdateStatus, VehicleStatus, VehicleSearch}
import org.genivi.sota.data.VehicleGenerators
import org.joda.time.DateTime
import org.scalatest.{FunSuite, ShouldMatchers}

class VehicleUpdateStatusSpec extends FunSuite
  with ShouldMatchers {

  import VehicleStatus._

  val vehicle = VehicleGenerators.genVehicle.sample.get.copy(lastSeen = Some(DateTime.now))

  test("Error if at least one package is in Failed State") {
    val packages = List(UpdateStatus.Failed, UpdateStatus.Finished)
    val result = VehicleSearch.currentVehicleStatus(vehicle.lastSeen, packages)
    result shouldBe Error
  }

  test("out of date if any package is not finished") {
    val packages = List(UpdateStatus.Pending, UpdateStatus.InFlight)
    val result = VehicleSearch.currentVehicleStatus(vehicle.lastSeen, packages)
    result shouldBe Outdated
  }

  test("up to date if all packages are Finished") {
    val packages = List(UpdateStatus.Finished, UpdateStatus.Finished)
    val result = VehicleSearch.currentVehicleStatus(vehicle.lastSeen, packages)
    result shouldBe UpToDate
  }

  test("not seen if vehicle was never seen") {
    val result = VehicleSearch.currentVehicleStatus(None, List.empty)
    result shouldBe NotSeen
  }
}
