package org.genivi.sota.core

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import io.circe.generic.auto._
import io.circe.{Encoder, Json}
import java.util.UUID
import org.genivi.sota.core.data.client.PendingUpdateRequest
import org.genivi.sota.core.data.{Package, UpdateRequest, UpdateStatus}
import org.genivi.sota.core.db.{InstallHistories, Packages}
import org.genivi.sota.core.resolver.{Connectivity, ConnectivityClient, DefaultConnectivity}
import org.genivi.sota.core.rvi.{InstallReport, OperationResult, UpdateReport}
import org.genivi.sota.core.transfer.DeviceUpdates
import org.genivi.sota.data.{Device, DeviceGenerators, PackageIdGenerators, VehicleGenerators}
import org.genivi.sota.datatype.NamespaceDirective
import java.time.Instant
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSuite, Inspectors, ShouldMatchers}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class DeviceUpdatesResourceSpec extends FunSuite
  with ShouldMatchers
  with ScalatestRouteTest
  with ScalaFutures
  with DatabaseSpec
  with Inspectors
  with UpdateResourcesDatabaseSpec {

  import NamespaceDirective._
  import Device._
  import DeviceGenerators._
  import PackageIdGenerators._
  import VehicleGenerators._

  implicit val patience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  implicit val _db = db
  implicit val connectivity = new FakeConnectivity()

  lazy val service = new DeviceUpdatesResource(db, fakeResolver, fakeDeviceRegistry, defaultNamespaceExtractor)

  val fakeResolver = new FakeExternalResolver

  val baseUri = Uri.Empty.withPath(Path("/api/v1/vehicle_updates"))
  val (fakeDeviceRegistry, deviceUri, deviceUuid) = {
    val registry = new FakeDeviceRegistry()
    val deviceId = DeviceId(genVin.sample.get.get) // TODO unify Vins and DeviceIds
    val device = genDeviceT.sample.get.copy(deviceId = Some(deviceId))
    val id = Await.result(registry.createDevice(device), 1.seconds)
    (registry, Uri.Empty.withPath(baseUri.path / id.underlying.get), id)
  }

  test("install updates are forwarded to external resolver") {
    val packageIds = Gen.listOf(genPackageId).sample.get
    val uri = Uri.Empty.withPath(deviceUri.path / "installed")

    Put(uri, packageIds) ~> service.route ~> check {
      status shouldBe StatusCodes.NoContent

      packageIds.foreach { p =>
        fakeResolver.installedPackages.toList should contain(p)
      }
    }
  }

  test("GET to download file returns the file contents") {
    whenReady(createUpdateSpec()) { case (packageModel, device, updateSpec) =>
      val url = Uri.Empty.withPath(baseUri.path / device.id.underlying.get / updateSpec.request.id.toString / "download")

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        responseEntity.contentLengthOption should contain(packageModel.size)
      }
    }
  }

  test("GET returns 404 if there is no package with the given id") {
    val uuid = genId.sample.get
    val url = Uri.Empty.withPath(deviceUri.path / uuid.underlying.get / "download")

    Get(url) ~> service.route ~> check {
      status shouldBe StatusCodes.NotFound
      responseAs[String] should include("Package not found")
    }
  }

  test("GET update requests for a device returns a list of PendingUpdateResponse") {
    whenReady(createUpdateSpec()) { case (_, device, updateSpec) =>
      val uri = Uri.Empty.withPath(baseUri.path / device.id.underlying.get)

      Get(uri) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val parsedResponse = responseAs[List[PendingUpdateRequest]]
        parsedResponse shouldNot be(empty)
        parsedResponse.map(_.requestId) should be(List(updateSpec.request.id))
        parsedResponse.map(_.packageId) should be(List(updateSpec.request.packageId))
      }
    }
  }

  test("sets device last seen when device asks for updates") {
    val now = Instant.now().minusSeconds(10)

    Get(deviceUri) ~> service.route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[List[UUID]] should be(empty)

      val device = fakeDeviceRegistry.devices.find(_.id == deviceUuid)

      device match {
        case Some(d) =>
          d.lastSeen shouldBe defined
          d.lastSeen.get.isAfter(now) shouldBe true
        case _ =>
          fail("Device should be in database")
      }
    }
  }

  test("POST an update report updates an UpdateSpec status") {
    whenReady(createUpdateSpec()) { case (_, device, updateSpec) =>
      val url = Uri.Empty.withPath(baseUri.path / device.id.underlying.get / updateSpec.request.id.toString)
      val result = OperationResult("opid", 1, "some result")
      val updateReport = UpdateReport(updateSpec.request.id, List(result))
      val installReport = InstallReport(device.id, updateReport)

      Post(url, installReport) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent

        val dbIO = for {
          updateSpec <- DeviceUpdates.findUpdateSpecFor(device.id, updateSpec.request.id)
          histories <- InstallHistories.list(device.namespace, device.id)
        } yield (updateSpec, histories.last)

        whenReady(db.run(dbIO)) { case (updatedSpec, lastHistory) =>
          updatedSpec.status shouldBe UpdateStatus.Finished
          lastHistory.success shouldBe true
        }
      }
    }
  }

  test("Returns 404 if package does not exist") {
    val fakeUpdateRequestUuid = UUID.randomUUID()
    val url = Uri.Empty.withPath(deviceUri.path / fakeUpdateRequestUuid.toString)
    val result = OperationResult(UUID.randomUUID().toString, 1, "some result")
    val updateReport = UpdateReport(fakeUpdateRequestUuid, List(result))
    val installReport = InstallReport(deviceUuid, updateReport)

    Post(url, installReport) ~> service.route ~> check {
      status shouldBe StatusCodes.NotFound
      responseAs[String] should include("Could not find an update request with id ")
    }
  }

  test("GET to download a file returns 3xx if the package URL is an s3 URI") {
    val service = new DeviceUpdatesResource(db, fakeResolver, fakeDeviceRegistry, defaultNamespaceExtractor) {
      override lazy val packageRetrievalOp: (Package) => Future[HttpResponse] = {
        _ => Future.successful {
          HttpResponse(StatusCodes.Found, Location("https://some-fake-place") :: Nil)
        }
      }
    }

    val f = for {
      (packageModel, device, updateSpec) <- createUpdateSpec()
      _ <- db.run(Packages.create(packageModel.copy(uri = "https://amazonaws.com/file.rpm")))
    } yield (packageModel, device, updateSpec)

    whenReady(f) { case (packageModel, device, updateSpec) =>
      val url = Uri.Empty.withPath(baseUri.path / device.id.underlying.get / updateSpec.request.id.toString / "download")
      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.Found
        header("Location").map(_.value()) should contain("https://some-fake-place")
      }
    }
  }


  test("POST on queues a package for update to a specific vehile") {
    val f = createUpdateSpec()

    whenReady(f) { case (packageModel, device, updateSpec) =>
      val now = Instant.now
      val url = Uri.Empty.withPath(baseUri.path / device.id.underlying.get)

      Post(url, packageModel.id) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val updateRequest = responseAs[UpdateRequest]
        updateRequest.packageId should be (packageModel.id)
        updateRequest.creationTime.isAfter(now) shouldBe true
      }
    }
  }

  test("POST on /:id/sync results in an rvi sync") {
    val url = Uri.Empty.withPath(deviceUri.path / "sync")
    Post(url) ~> service.route ~> check {
      status shouldBe StatusCodes.NoContent

      val service = s"genivi.org/device/${deviceUuid.underlying.get}/sota/getpackages"

      connectivity.sentMessages should contain(service -> Json.Null)
    }
  }

  test("PUT to set install order should change the package install order") {
    val dbio = for {
      (_, d, spec0) <- createUpdateSpecAction()
      (_, spec1) <- createUpdateSpecFor(d.id)
    } yield (d, spec0.request.id, spec1.request.id)

    whenReady(db.run(dbio)) { case (d, spec0, spec1) =>
      val url = Uri.Empty.withPath(baseUri.path / d.id.underlying.get / "order")
      val deviceUrl = Uri.Empty.withPath(baseUri.path / d.id.underlying.get)
      val req = Map(0 -> spec1, 1 -> spec0)

      Put(url, req) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent

        Get(deviceUrl) ~> service.route ~> check {
          val pendingUpdates =
            responseAs[List[PendingUpdateRequest]].sortBy(_.installPos).map(_.requestId)

          pendingUpdates shouldBe List(spec1, spec0)
        }
      }
    }
  }

  test("can cancel pending updates") {
    whenReady(createUpdateSpec()) { case (_, device, updateSpec) =>
      val url = Uri.Empty.withPath(baseUri.path / device.id.underlying.get / updateSpec.request.id.toString / "cancelupdate")
      Put(url) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent

        whenReady(db.run(DeviceUpdates.findUpdateSpecFor(device.id, updateSpec.request.id))) { case updateSpec =>
          updateSpec.status shouldBe UpdateStatus.Canceled
        }
      }
    }
  }

  test("GET update results for a vehicle returns a list of OperationResults") {
    whenReady(createUpdateSpec()) { case (_, vehicle, updateSpec) =>
      val uri = deviceUri.withPath(deviceUri.path / "results")

      Get(uri) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val parsedResponse = responseAs[List[OperationResult]]
        parsedResponse should be(empty)
      }
    }
  }

  test("GET update results for an update request returns a list of OperationResults") {
    whenReady(createUpdateSpec()) { case (_, vehicle, updateSpec) =>
      val uri = deviceUri.withPath(deviceUri.path / updateSpec.request.id.toString / "results")

      Get(uri) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val parsedResponse = responseAs[List[OperationResult]]
        parsedResponse should be(empty)
      }
    }
  }

}

class FakeConnectivity extends Connectivity {

  val sentMessages = scala.collection.mutable.Queue.empty[(String, Json)]

  override implicit val transport = { (_: Json) =>
    Future.successful(Json.Null)
  }

  override implicit val client = new ConnectivityClient {
    override def sendMessage[A](service: String, message: A, expirationDate: Instant)
                               (implicit encoder: Encoder[A]): Future[Int] = {
      val v = (service, encoder(message))
      sentMessages += v

      Future.successful(0)
    }
  }
}
