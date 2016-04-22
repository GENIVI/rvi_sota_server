/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.genivi.sota.core.{DatabaseSpec, DefaultDBPatience, UpdateResourcesDatabaseSpec}
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}

import scala.concurrent.ExecutionContext

class UpdateSpecsSpec extends FunSuite
  with ShouldMatchers
  with UpdateResourcesDatabaseSpec
  with ScalaFutures
  with DatabaseSpec
  with DefaultDBPatience
   {

  import UpdateSpecs._

  implicit val ec = ExecutionContext.global

  test("when multiple packages are pending, sort them by FIFO") {
    val secondCreationTime = DateTime.now.plusHours(1)

    val f = for {
      (package0, vehicle, _) <- createUpdateSpec()
      (package1, _) <- db.run(createUpdateSpecFor(vehicle, secondCreationTime))
      result <- db.run(getPackagesQueuedForVin(defaultNamespace, vehicle.vin))
    } yield (result, package0, package1)

    whenReady(f) { case (result, package0, package1)  =>
      result shouldBe Vector(package0.id, package1.id)
    }
  }
}
