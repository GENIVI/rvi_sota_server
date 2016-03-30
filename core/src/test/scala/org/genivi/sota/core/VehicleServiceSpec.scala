package org.genivi.sota.core

import akka.http.scaladsl.unmarshalling.Unmarshaller._
import java.util.UUID
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.genivi.sota.core.rvi.{InstallReport, OperationResult, UpdateReport}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, Inspectors, ShouldMatchers}
import org.genivi.sota.data.VehicleGenerators._
import org.genivi.sota.data.PackageIdGenerators._
import org.scalacheck.Gen
import org.scalatest.time.{Millis, Seconds, Span}
import org.genivi.sota.core.data.UpdateStatus
import org.genivi.sota.core.db.{InstallHistories, Vehicles}
import org.genivi.sota.core.transfer.InstalledPackagesUpdate
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import io.circe.generic.auto._

class VehicleServiceSpec extends FunSuite
  with ShouldMatchers
  with ScalatestRouteTest
  with ScalaFutures
  with DatabaseSpec
  with Inspectors
  with UpdateResourcesDatabaseSpec {

  val fakeResolver = new FakeExternalResolver()

  implicit val connectivity = DefaultConnectivity

  lazy val service = new VehicleService(db, fakeResolver)

  val BasePath = Path("/api/v1/vehicles")

  implicit val patience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  implicit val _db = db

  test("install updates are forwarded to external resolver") {
    val fakeResolverClient = new FakeExternalResolver()
    val vehiclesResource = new VehicleService(db, fakeResolverClient)

    val vin = genVehicle.sample.get.vin
    val packageIds = Gen.listOf(genPackageId).sample.get

    val uri = Uri.Empty.withPath(BasePath / vin.get / "updates")

    Post(uri, packageIds) ~> vehiclesResource.route ~> check {
      status shouldBe StatusCodes.NoContent

      packageIds.foreach { p =>
        fakeResolverClient.installedPackages.toList should contain(p)
      }
    }
  }

  test("GET to download file returns the file contents") {
    whenReady(createUpdateSpec()) { case (packageModel, vehicle, updateSpec) =>
      val url = Uri.Empty.withPath(BasePath / vehicle.vin.get / "updates" / updateSpec.request.id.toString / "download")
      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        responseEntity.contentLengthOption should contain(packageModel.size)
      }
    }
  }

  test("GET returns 404 if there is no package with the given id") {
    val vehicle = genVehicle.sample.get
    val uuid = UUID.randomUUID()
    val url = Uri.Empty.withPath(BasePath / vehicle.vin.get / "updates" / uuid.toString / "download")

    Get(url) ~> service.route ~> check {
      status shouldBe StatusCodes.NotFound
      responseAs[String] should include("Package not found")
    }
  }

  test("GET update requests for a vehicle returns a list of UUIDS") {
    whenReady(createUpdateSpec()) { case (_, vehicle, updateSpec) =>
      val url = Uri.Empty.withPath(BasePath / vehicle.vin.get / "updates")

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[List[UUID]] shouldNot be(empty)
        responseAs[List[UUID]] should be(List(updateSpec.request.id))
      }
    }
  }

  test("POST an update report updates an UpdateSpec status") {
    whenReady(createUpdateSpec()) { case (_, vehicle, updateSpec) =>
      val url = Uri.Empty.withPath(BasePath / vehicle.vin.get / "updates" / updateSpec.request.id.toString)
      val result = OperationResult("opid", 1, "some result")
      val updateReport = UpdateReport(updateSpec.request.id, List(result))
      val installReport = InstallReport(vehicle.vin, updateReport)

      Post(url, installReport) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent

        val dbIO = for {
          updateSpec <- InstalledPackagesUpdate.findUpdateSpecFor(vehicle.vin, updateSpec.request.id)
          histories <- InstallHistories.list(vehicle.vin)
        } yield (updateSpec, histories.last)

        whenReady(db.run(dbIO)) { case (updatedSpec, lastHistory) =>
          updatedSpec.status shouldBe UpdateStatus.Finished
          lastHistory.success shouldBe true
        }
      }
    }
  }

  test("Returns 404 if package does not exist") {
    val vehicle = genVehicle.sample.get
    val f = db.run(Vehicles.create(vehicle))

    whenReady(f) { vin =>
      val fakeUpdateRequestUuid = UUID.randomUUID()
      val url = Uri.Empty.withPath(BasePath / vin.get / "updates" / fakeUpdateRequestUuid.toString)
      val result = OperationResult(UUID.randomUUID().toString, 1, "some result")
      val updateReport = UpdateReport(fakeUpdateRequestUuid, List(result))
      val installReport = InstallReport(vehicle.vin, updateReport)

      Post(url, installReport) ~> service.route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[String] should include("Could not find an update request with id ")
      }
    }
  }
}
