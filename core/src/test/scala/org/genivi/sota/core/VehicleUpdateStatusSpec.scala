package org.genivi.sota.core

import java.util.UUID

import org.genivi.sota.core.data.{UpdateStatus, VehicleStatus, VehicleUpdateStatus}
import org.genivi.sota.data.VehicleGenerators
import org.joda.time.DateTime
import org.scalatest.{FunSuite, ShouldMatchers}

class VehicleUpdateStatusSpec extends FunSuite
  with ShouldMatchers {

  import VehicleStatus._

  val vehicle = VehicleGenerators.genVehicle.sample.get.copy(lastSeen = Some(DateTime.now))

  def uuid = UUID.randomUUID()

  test("Error if at least one package is in Failed State") {
    val packages = List((uuid, UpdateStatus.Failed), (uuid, UpdateStatus.Finished))
    val result = VehicleUpdateStatus.current(vehicle, packages).status
    result shouldBe Error
  }

  test("Always includes a vehicle last seen date") {
    val result = VehicleUpdateStatus.current(vehicle, List.empty)
    result.lastSeen shouldBe vehicle.lastSeen
  }

  test("out of date if any package is not finished") {
    val packages = List((uuid, UpdateStatus.Pending), (uuid, UpdateStatus.InFlight))
    val result = VehicleUpdateStatus.current(vehicle, packages).status
    result shouldBe Outdated
  }

  test("up to date if all packages are Finished") {
    val packages = List((uuid, UpdateStatus.Finished), (uuid, UpdateStatus.Finished))
    val result = VehicleUpdateStatus.current(vehicle, packages).status
    result shouldBe UpToDate
  }

  test("not seen if vehicle was never seen") {
    val result = VehicleUpdateStatus.current(vehicle.copy(lastSeen = None), List.empty).status
    result shouldBe NotSeen
  }
}
