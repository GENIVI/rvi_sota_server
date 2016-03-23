package org.genivi.sota.core.transfer

import org.genivi.sota.core.DatabaseSpec
import org.genivi.sota.data.VehicleGenerators
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

class PackageDownloadProcessSpec extends FunSuite
  with ShouldMatchers
  with DatabaseSpec
  with ScalaFutures {

  implicit val ec = scala.concurrent.ExecutionContext.global

  val packageDownloadProcess = new PackageDownloadProcess(db)

  test("builds a response with an empty list if there are no pending updates for a vehile") {
    val vin = VehicleGenerators.genVin.sample.get
    val pendingIdsResponse = packageDownloadProcess.buildClientPendingIdsResponse(vin)

    whenReady(pendingIdsResponse) { uuids =>
      uuids shouldBe empty
    }
  }
}
