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
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.{BlacklistedPackages, InstallHistories, Packages, UpdateSpecs}
import org.genivi.sota.core.resolver.{Connectivity, ConnectivityClient}
import org.genivi.sota.core.rvi.{InstallReport, OperationResult, UpdateReport}
import org.genivi.sota.core.transfer.DeviceUpdates
import org.genivi.sota.data._
import java.time.Instant

import akka.testkit.TestKitBase
import eu.timepit.refined.api.Refined
import org.genivi.sota.http.NamespaceDirectives
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.messaging.{LocalMessageBus, MessageBusPublisher}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSuite, Inspectors, ShouldMatchers}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import cats.syntax.show._
import org.genivi.sota.messaging.Messages.{BandwidthUsage, DeviceSeen}

class DeviceUpdatesResourceSpec extends FunSuite
  with ShouldMatchers
  with ScalatestRouteTest
  with ScalaFutures
  with DatabaseSpec
  with Inspectors
  with UpdateResourcesDatabaseSpec
  with LongRequestTimeout
  with TestKitBase {

  import Arbitrary._
  import NamespaceDirectives._
  import DeviceGenerators._
  import PackageIdGenerators._
  import UpdateSpec._
  import UuidGenerator._

  implicit val patience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  implicit val connectivity = new FakeConnectivity()

  lazy val service = new DeviceUpdatesResource(db, fakeResolver, fakeDeviceRegistry, None,
    defaultNamespaceExtractor, LocalMessageBus.publisher(system))

  val fakeResolver = new FakeExternalResolver()

  val baseUri = Uri.Empty.withPath(Path("/api/v1/device_updates"))
  val (fakeDeviceRegistry, deviceUri, deviceUuid) = {
    val registry = new FakeDeviceRegistry(Namespaces.defaultNs)
    val device = genDeviceT.sample.get
    val id = Await.result(registry.createDevice(device), 1.seconds)
    (registry, Uri.Empty.withPath(baseUri.path / id.underlying.get), id)
  }

  def createDeviceAndUpdateSpec(installPos: Int = 0, withMillis: Long = -1)
    (implicit ec: ExecutionContext): DBIO[(Package, Uuid, UpdateSpec)] = {
    val device = genDeviceT.sample.get
    val id = Await.result(fakeDeviceRegistry.createDevice(device), 1.seconds)
    for {
      (packageModel, updateSpec) <- createUpdateSpecFor(id, installPos, withMillis)
    } yield (packageModel, id, updateSpec)
  }

  val id = Uuid(Refined.unsafeApply(UUID.randomUUID().toString))

  // Deprecated, see newer 'mydevice' test below
  test("install updates are forwarded to external resolver") {
    val packageIds = Gen.listOf(genPackageId).sample.get
    val uri = Uri.Empty.withPath(deviceUri.path / "installed")

    Put(uri, packageIds) ~> service.route ~> check {
      status shouldBe StatusCodes.OK

      packageIds.foreach { p =>
        fakeDeviceRegistry.isInstalled(deviceUuid, p) shouldBe true
      }
    }
  }

  // Deprecated, see newer 'mydevice' test below
  test("system_info updates are forwarded to device registry") {
    import SimpleJsonGenerator._

    val newSystemInfo = simpleJsonGen.sample.get
    val uri = Uri.Empty.withPath(deviceUri.path / "system_info")

    Put(uri, newSystemInfo) ~> service.route ~> check {
      status shouldBe StatusCodes.OK

      val systemInfo = fakeDeviceRegistry.getSystemInfo(deviceUuid).futureValue
      newSystemInfo shouldBe systemInfo
    }

  }

  // Deprecated, see newer 'mydevice' test below
  test("GET to download file returns the file contents") {
    whenReady(createUpdateSpec()) { case (packageModel, device, updateSpec) =>
      val url =
        Uri.Empty.withPath(baseUri.path / device.uuid.underlying.get / updateSpec.request.id.toString / "download")

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        responseEntity.contentLengthOption should contain(packageModel.size)
      }
    }
  }

  // Deprecated, see newer 'mydevice' test below
  test("GET returns 404 if there is no package with the given id") {
    val uuid = arbitrary[Uuid].sample.get
    val url = Uri.Empty.withPath(deviceUri.path / uuid.underlying.get / "download")

    Get(url) ~> service.route ~> check {
      status shouldBe StatusCodes.NotFound
      responseAs[String] should include("package not found")
    }
  }

  // Deprecated, see newer 'mydevice' test below
  test("GET update requests for a device returns a list of PendingUpdateResponse") {
    whenReady(createUpdateSpec()) { case (pkg, device, updateSpec) =>
      val uri = Uri.Empty.withPath(baseUri.path / device.uuid.show)

      Get(uri) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val parsedResponse = responseAs[List[PendingUpdateRequest]]
        parsedResponse shouldNot be(empty)
        parsedResponse.map(_.requestId) should be(List(updateSpec.request.id))
        parsedResponse.map(_.packageId) should be(List(pkg.id))
      }
    }
  }

  // Deprecated, see newer 'mydevice' test below
  test("sets device last seen when device asks for updates") {
    system.eventStream.subscribe(testActor, classOf[DeviceSeen])
    val now = Instant.now().minusSeconds(10)

    Get(deviceUri) ~> service.route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[List[UUID]] should be(empty)
    }

    expectMsgPF(10.seconds, s"device seen message is sent for $deviceUuid") {
      case m@DeviceSeen(_, device, lastSeen)
        if device == deviceUuid && lastSeen.isAfter(now) => m
    }
    system.eventStream.unsubscribe(testActor, classOf[DeviceSeen])
  }

  // Deprecated, see newer 'mydevice' test below
  test("POST an update report updates an UpdateSpec status") {
    whenReady(createUpdateSpec()) { case (packageModel, device, updateSpec) =>
      val url = Uri.Empty.withPath(baseUri.path / device.uuid.show / updateSpec.request.id.toString)
      val result = OperationResult(id, 1, "some result")
      val updateReport = UpdateReport(updateSpec.request.id, List(result))
      val installReport = InstallReport(device.uuid, updateReport)

      Post(url, installReport) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent

        val dbIO = for {
          updateSpec <- DeviceUpdates.findUpdateSpecFor(device.uuid, updateSpec.request.id)
          histories <- InstallHistories.list(device.uuid)
        } yield (updateSpec, histories.last)

        whenReady(db.run(dbIO)) { case (updatedSpec, lastHistory) =>
          updatedSpec.status shouldBe UpdateStatus.Finished
          lastHistory._1.success shouldBe true
          lastHistory._2.head shouldBe packageModel.id
        }
      }
    }
  }

  // Deprecated, see newer 'mydevice' test below
  test("Returns 404 if package does not exist") {
    val fakeUpdateRequestUuid = UUID.randomUUID()
    val url = Uri.Empty.withPath(deviceUri.path / fakeUpdateRequestUuid.toString)
    val result = OperationResult(id, 1, "some result")
    val updateReport = UpdateReport(fakeUpdateRequestUuid, List(result))
    val installReport = InstallReport(deviceUuid, updateReport)

    Post(url, installReport) ~> service.route ~> check {
      status shouldBe StatusCodes.NotFound
      responseAs[String] should include("UpdateSpec not found")
    }
  }

  // Deprecated, see newer 'mydevice' test below
  test("GET to download a file returns 3xx if the package URL is an s3 URI") {
    val service = new DeviceUpdatesResource(db, fakeResolver, fakeDeviceRegistry, None,
      defaultNamespaceExtractor, MessageBusPublisher.ignore) {
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
      val url =
        Uri.Empty.withPath(baseUri.path / device.uuid.underlying.get / updateSpec.request.id.toString / "download")
      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.Found
        header("Location").map(_.value()) should contain("https://some-fake-place")
      }
    }
  }


  test("POST queues a package for update to a specific vehicle") {
    whenReady(db.run(createDeviceAndUpdateSpec())) { case (packageModel, devUuid, updateSpec) =>
      val now = Instant.now
      val url = Uri.Empty.withPath(baseUri.path / devUuid.underlying.get)

      Post(url, packageModel.id) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val updateRequest = responseAs[PendingUpdateRequest]
        updateRequest.packageId shouldBe packageModel.id
        updateRequest.createdAt.isAfter(now) shouldBe true
      }
    }
  }

  test("update requests with blacklisted packages are excluded from queue") {
    val f = for {
      (p, d, us) <- db.run(createDeviceAndUpdateSpec())
      _ <- BlacklistedPackages.create(p.namespace, p.id, None)
    } yield (p, d, us)

    whenReady(f) { case (packageModel, devUuid, updateSpec) =>
      val url = baseUri.withPath(baseUri.path / devUuid.underlying.get / "queued")

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        val resp = responseAs[List[PendingUpdateRequest]]

        resp shouldBe empty
      }
    }
  }

  test("queue items includes update status") {
    whenReady(db.run(createDeviceAndUpdateSpec())) { case (packageModel, devUuid, updateSpec) =>
      val url = baseUri.withPath(baseUri.path / devUuid.underlying.get / "queued")

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        val resp = responseAs[List[PendingUpdateRequest]]

        resp.map(_.status) should contain(updateSpec.status)
      }
    }
  }

  test("returns in flight updates for /queued endpoint") {
    val f = for {
      (_, d, us) <- db.run(createDeviceAndUpdateSpec())
      _ <- db.run(UpdateSpecs.setStatus(us, UpdateStatus.InFlight))
    } yield (d, us)

    whenReady(f) { case (devUuid, us) =>
      val url = baseUri.withPath(baseUri.path / devUuid.underlying.get / "queued")

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        val resp = responseAs[List[PendingUpdateRequest]]
        resp.map(_.requestId) should contain(us.request.id)
      }
    }
  }

  test("updates updated_time field is returned to client") {
    val now = Instant.now.minusSeconds(12 * 3600)

    val f = for {
      (pkg, devUuid, us) <- createDeviceAndUpdateSpec(installPos = 0, withMillis = now.toEpochMilli)
      _ <- UpdateSpecs.setStatus(us, UpdateStatus.InFlight)
    } yield (pkg, devUuid, us)

    whenReady(db.run(f)) { case (pkg, devUuid, us) =>
      val url = baseUri.withPath(baseUri.path / devUuid.underlying.get / "queued")

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        val resp = responseAs[List[PendingUpdateRequest]]
        resp.map(_.requestId) should contain(us.request.id)

        val pendingUpdate = resp.find(_.requestId == us.request.id)

        pendingUpdate.map(_.updatedAt.isAfter(now)) should contain(true)
      }
    }
  }

  // Deprecated, see newer 'mydevice' test below
  test("downloading an update with a blacklisted package returns bad request") {
    val f = for {
      (p, d, us) <- createUpdateSpec()
      _ <- BlacklistedPackages.create(p.namespace, p.id, None)
    } yield (p, d, us)

    whenReady(f) { case (packageModel, device, updateSpec) =>
      val url =
        baseUri
          .withPath(baseUri.path / device.uuid.show / updateSpec.request.id.toString / "download" )

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.BadRequest
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
      (_, devUuid, spec0) <- createDeviceAndUpdateSpec()
      (_, spec1) <- createUpdateSpecFor(devUuid)
    } yield (devUuid, spec0.request.id, spec1.request.id)

    whenReady(db.run(dbio)) { case (devUuid, spec0, spec1) =>
      val url = Uri.Empty.withPath(baseUri.path / devUuid.underlying.get / "order")
      val deviceUrl = Uri.Empty.withPath(baseUri.path / devUuid.underlying.get)
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
    whenReady(db.run(createDeviceAndUpdateSpec())) { case (_, devUuid, updateSpec) =>
      val url =
        Uri.Empty.withPath(baseUri.path / devUuid.underlying.get / updateSpec.request.id.toString / "cancelupdate")
      Put(url) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent

        whenReady(db.run(DeviceUpdates.findUpdateSpecFor(devUuid, updateSpec.request.id))) { updateSpec =>
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

  test("PUT to block and unblock the update queue") {
      val url = Uri.Empty.withPath(deviceUri.path / "blocked")
      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[List[BlockedInstall]] should be(empty)
      }

      Put(url) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent
      }

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[List[BlockedInstall]] shouldNot be(empty)
      }

      Delete(url) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent
      }

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[List[BlockedInstall]] should be(empty)
      }
  }

  ignore("after blocking installation queue, no packages are returned even if some are pending for installation") {
    whenReady(createUpdateSpec()) { case (packageModel, device, updateSpec) =>
      // Block the installation queue of the device
      val blockUrl = Uri.Empty.withPath(deviceUri.path / "blocked")
      Put(blockUrl) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent
      }

      // Check zero packages are returned for install
      val url = baseUri.withPath(baseUri.path / device.uuid.underlying.get / "queued")
      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val parsedResponse = responseAs[List[PendingUpdateRequest]]
        parsedResponse should be(empty)
      }

      // Unblock the installation queue
      Delete(blockUrl) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent
      }

      // Check the pending package is returned for install
      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val parsedResponse = responseAs[List[PendingUpdateRequest]]
        parsedResponse.size shouldBe 1
        val pendingReq = parsedResponse.head
        packageModel.id shouldBe pendingReq.packageId
      }
    }
  }

  val mydeviceUri = Uri.Empty.withPath(Path("/api/v1/mydevice"))

  test("Device PUT installed packages are forwarded to external resolver") {
    val packageIds = Gen.listOf(genPackageId).sample.get
    val uri = Uri.Empty.withPath(mydeviceUri.path / deviceUuid.underlying.get / "installed")

    Put(uri, packageIds) ~> service.route ~> check {
      status shouldBe StatusCodes.OK

      packageIds.foreach { p =>
        fakeDeviceRegistry.isInstalled(deviceUuid, p) shouldBe true
      }
    }
  }

  test("Device PUT system_info are forwarded to device registry") {
    import SimpleJsonGenerator._

    val newSystemInfo = simpleJsonGen.sample.get
    val uri = Uri.Empty.withPath(mydeviceUri.path / deviceUuid.underlying.get / "system_info")

    Put(uri, newSystemInfo) ~> service.route ~> check {
      status shouldBe StatusCodes.OK

      val systemInfo = fakeDeviceRegistry.getSystemInfo(deviceUuid).futureValue
      newSystemInfo shouldBe systemInfo
    }
  }

  test("Device GET download returns the file contents") {
    whenReady(createUpdateSpec()) { case (packageModel, device, updateSpec) =>
      val url =
        Uri.Empty.withPath(mydeviceUri.path / device.uuid.underlying.get / "updates" / updateSpec.request.id.toString / "download")

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK

        responseEntity.contentLengthOption should contain(packageModel.size)
      }
    }
  }

  test("Device GET download returns 404 if there is no update with the given id") {
    val uuid = arbitrary[Uuid].sample.get
    val url = Uri.Empty.withPath(mydeviceUri.path / deviceUuid.underlying.get / "updates" / uuid.underlying.get / "download")

    Get(url) ~> service.route ~> check {
      status shouldBe StatusCodes.NotFound
      responseAs[String] should include("package not found")
    }
  }

  test("Device GET updates returns a list of PendingUpdateResponse") {
    whenReady(createUpdateSpec()) { case (pkg, device, updateSpec) =>
      val uri = Uri.Empty.withPath(mydeviceUri.path / device.uuid.underlying.get / "updates")

      Get(uri) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val parsedResponse = responseAs[List[PendingUpdateRequest]]
        parsedResponse shouldNot be(empty)
        parsedResponse.map(_.requestId) should be(List(updateSpec.request.id))
        parsedResponse.map(_.packageId) should be(List(pkg.id))
      }
    }
  }

  test("Device GET updates sets last-seen") {
    system.eventStream.subscribe(testActor, classOf[DeviceSeen])

    val now = Instant.now().minusSeconds(10)

    val uri = Uri.Empty.withPath(mydeviceUri.path / deviceUuid.underlying.get / "updates")
    Get(uri) ~> service.route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[List[UUID]] should be(empty)
    }

    expectMsgPF(10.seconds, s"device seen message is sent for $deviceUuid") {
      case m@DeviceSeen(_, device, lastSeen)
        if device == deviceUuid && lastSeen.isAfter(now) => m
    }
    system.eventStream.unsubscribe(testActor, classOf[DeviceSeen])
  }

  test("Device POST update report set an UpdateSpec status") {
    whenReady(createUpdateSpec()) { case (packageModel, device, updateSpec) =>
      val url = Uri.Empty.withPath(mydeviceUri.path / device.uuid.show / "updates" / updateSpec.request.id.toString)
      val result = OperationResult(id, 1, "some result")

      Post(url, List(result)) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent

        val dbIO = for {
          updateSpec <- DeviceUpdates.findUpdateSpecFor(device.uuid, updateSpec.request.id)
          histories <- InstallHistories.list(device.uuid)
        } yield (updateSpec, histories.last)

        whenReady(db.run(dbIO)) { case (updatedSpec, lastHistory) =>
          updatedSpec.status shouldBe UpdateStatus.Finished
          lastHistory._1.success shouldBe true
          lastHistory._2.head shouldBe packageModel.id
        }
      }
    }
  }

  test("Device GET an update returns 404 if update does not exist") {
    val fakeUpdateRequestUuid = UUID.randomUUID()
    val url = Uri.Empty.withPath(mydeviceUri.path / deviceUuid.underlying.get / "updates" / fakeUpdateRequestUuid.toString)
    val result = OperationResult(id, 1, "some result")

    Post(url, List(result)) ~> service.route ~> check {
      status shouldBe StatusCodes.NotFound
      responseAs[String] should include("UpdateSpec not found")
    }
  }

  test("Device GET download returns 3xx if the package URL is an s3 URI") {
    val service = new DeviceUpdatesResource(db, fakeResolver, fakeDeviceRegistry, None,
      defaultNamespaceExtractor, MessageBusPublisher.ignore) {
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
      val url =
        Uri.Empty.withPath(mydeviceUri.path / device.uuid.underlying.get / "updates" / updateSpec.request.id.toString / "download")
      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.Found
        header("Location").map(_.value()) should contain("https://some-fake-place")
      }
    }
  }

  test("Device GET download an update with a blacklisted package returns bad request") {
    val f = for {
      (p, d, us) <- createUpdateSpec()
      _ <- BlacklistedPackages.create(p.namespace, p.id, None)
    } yield (p, d, us)

    whenReady(f) { case (packageModel, device, updateSpec) =>
      val url =
        baseUri
          .withPath(mydeviceUri.path / device.uuid.show / "updates" / updateSpec.request.id.toString / "download" )

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }
  }

  test("publishes message to bus on fetch") {
    system.eventStream.subscribe(testActor, classOf[BandwidthUsage])

    whenReady(createUpdateSpec()) { case (packageModel, device, updateSpec) =>
      val url =
        Uri.Empty.withPath(mydeviceUri.path / device.uuid.underlying.get / "updates" / updateSpec.request.id.toString / "download")

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
      }

      expectMsgPF(10.seconds, "usage equal to pkg size and source equal to pkg id") {
        case m@BandwidthUsage(_, _, _, usage, updateType, source)
          if updateType == UpdateType.Package && usage == packageModel.size && source == packageModel.id.show => m
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
