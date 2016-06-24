package org.genivi.sota.core.transfer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.genivi.sota.core._
import org.genivi.sota.core.data.{UpdateSpec, UpdateStatus}
import org.genivi.sota.core.db._
import org.genivi.sota.core.rvi.OperationResult
import org.genivi.sota.core.rvi.UpdateReport
import org.genivi.sota.data.DeviceGenerators
import org.genivi.sota.db.SlickExtensions
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.scalacheck.Gen
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._


class DeviceUpdatesSpec extends FunSuite
  with ShouldMatchers
  with BeforeAndAfterAll
  with Inspectors
  with ScalaFutures
  with DatabaseSpec
  with UpdateResourcesDatabaseSpec {

  import DeviceGenerators._
  import DeviceUpdates._
  import Generators._
  import SlickExtensions._
  import UpdateStatus._

  implicit val actorSystem = ActorSystem("InstalledPackagesUpdateSpec-ActorSystem")
  implicit val materializer = ActorMaterializer()
  val deviceRegistry = new FakeDeviceRegistry

  implicit val ec = ExecutionContext.global
  implicit val _db = db
  implicit val patience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  test("forwards request to resolver client") {
    val device = genDeviceT.sample.get.copy(deviceId = Some(genDeviceId.sample.get))
    whenReady(deviceRegistry.createDevice(device)) { id =>
      val resolverClient = new FakeExternalResolver
      val packageIds = Gen.listOf(PackageIdGen).sample.get
      val f = update(id, packageIds, resolverClient, deviceRegistry)

      whenReady(f) { _ =>
        forAll(packageIds) { id =>
          resolverClient.installedPackages should contain(id)
        }
      }
    }
  }

  test("marks reported packages as installed") {
    val f = for {
      (_, device, updateSpec) <- createUpdateSpec()
      result = OperationResult("opid", 1, "some result")
      report = UpdateReport(updateSpec.request.id, List(result))
      _ <- reportInstall(device.id, report)
      updatedSpec <- db.run(findUpdateSpecFor(device.id, updateSpec.request.id))
      history <- db.run(InstallHistories.list(device.namespace, device.id))
    } yield (updatedSpec.status, history)

    whenReady(f) { case (newStatus, history) =>
      newStatus should be(UpdateStatus.Finished)
      history.map(_.success) should contain(true)
    }
  }

  test("when multiple packages are pending sorted by installPos") {
    val dbIO = for {
      (_, device, updateSpec0) <- createUpdateSpecAction()
      (_, updateSpec1) <- createUpdateSpecFor(device.id, installPos = 2)
      result <- findPendingPackageIdsFor(device.id)
    } yield (result, updateSpec0, updateSpec1)

    whenReady(db.run(dbIO)) { case (result, updateSpec0, updateSpec1)  =>
      result shouldNot be(empty)
      result should have(size(2))

      result match {
        case Seq(first, second) =>
          first.id shouldBe updateSpec0.request.id
          second.id shouldBe updateSpec1.request.id
        case _ =>
          fail("returned package list does not have expected elements")
      }
    }
  }

  test("sets install priority for one package") {
    val dbIO = for {
      (pck, device, spec0) <- createUpdateSpecAction()
      (_, spec1) <- createUpdateSpecFor(device.id)
      _ <- persistInstallOrder(device.id, List(spec0.request.id, spec1.request.id))
      dbSpecs <- findPendingPackageIdsFor(device.id)
    } yield (dbSpecs, spec0, spec1)

    whenReady(db.run(dbIO)) { case (Seq(dbSpec0, dbSpec1), spec0, spec1) =>
      dbSpec0.id shouldBe spec0.request.id
      dbSpec0.installPos shouldBe 0

      dbSpec1.id shouldBe spec1.request.id
      dbSpec1.installPos shouldBe 1
    }
  }

  test("can only sort pending update requests") {
    import UpdateSpecs._
    import org.genivi.sota.refined.SlickRefined._

    val dbIO = for {
      (pck, device, spec0) <- createUpdateSpecAction()
      _ <- updateSpecs.filter(_.device === device.id).map(_.status).update(UpdateStatus.InFlight)
      (_, spec1) <- createUpdateSpecFor(device.id)
      result <- persistInstallOrder(device.id, List(spec0.request.id, spec1.request.id))
    } yield result

    val f = db.run(dbIO)

    whenReady(f.failed) { t =>
      t shouldBe a[SetOrderFailed]
      t.getMessage should include("need to be pending")
    }
  }

  test("fails when not specifying all update request in order") {
    val dbIO = for {
      (pck, d, spec0) <- createUpdateSpecAction()
      (_, spec1) <- createUpdateSpecFor(d.id)
      result <- persistInstallOrder(d.id, List(spec1.request.id))
    } yield result

    val f = db.run(dbIO)

    whenReady(f.failed) { t =>
      t shouldBe a[SetOrderFailed]
      t.getMessage should include("need to be specified")
    }
  }

  private def mkUpdateReport(updateSpec: UpdateSpec, isSuccess: Boolean): UpdateReport = {
    val result_code = if (isSuccess) 0 else 3
    val result = OperationResult("opid", result_code, "some result")
    UpdateReport(updateSpec.request.id, List(result))
  }

  test("rest of installation queue (consecutive installPos) remains Pending upon one package failing to install") {

    // insert update spec A install pos 0
    // insert update spec B install pos 1
    // insert update spec C install pos 2
    val dbIO = for {
      (_, device, updateSpec0) <- createUpdateSpecAction()
      (_, updateSpec1) <- createUpdateSpecFor(device.id, installPos = 1)
      (_, updateSpec2) <- createUpdateSpecFor(device.id, installPos = 2)
      result <- findPendingPackageIdsFor(device.id)
    } yield (result, updateSpec0, updateSpec1, updateSpec2)

    whenReady(db.run(dbIO)) { case (result, updateSpec0, updateSpec1, updateSpec2)  =>
      result shouldNot be(empty)
      result should have(size(3))

      // a different update request for each update spec
      val updateSpecs = List(updateSpec0, updateSpec1, updateSpec2)
      val reqIdsMatch = result.zip(updateSpecs).forall { case (ur, us) => ur.id == us.request.id }
      reqIdsMatch shouldBe true

      // all three update specs for the same device
      updateSpecs.map(_.device).toSet.size shouldBe 1

      // fail install for update spec B only
      val device = updateSpec0.device
      val f2 = for {
        _      <- reportInstall(device, mkUpdateReport(updateSpec0, isSuccess = true))
        _      <- reportInstall(device, mkUpdateReport(updateSpec1, isSuccess = false))
        usRow0 <- db.run(UpdateSpecs.findBy(updateSpec0))
        usRow1 <- db.run(UpdateSpecs.findBy(updateSpec1))
        usRow2 <- db.run(UpdateSpecs.findBy(updateSpec2))
      } yield (usRow0, usRow1, usRow2)

      whenReady(f2) { case (usRow0, usRow1, usRow2) =>
        // check update spec 0 status finished
        // check update spec 1 status failed
        // check update spec 2 status canceled
        val (_, _, vin0, status0, installPos0, _) = usRow0
        val (_, _, vin1, status1, installPos1, _) = usRow1
        val (_, _, vin2, status2, installPos2, _) = usRow2

        status0 shouldBe UpdateStatus.Finished
        status1 shouldBe UpdateStatus.Failed
        status2 shouldBe UpdateStatus.Pending
      }

    }
  }

  test("rest of installation queue (all installPos at 0) remains Pending upon one package failing to install") {

    // insert update spec A install pos 0 (ie, installation order to be disambiguated by creationTime)
    // insert update spec B install pos 0
    // insert update spec C install pos 0

    val dbIO = for {
      (_, device, updateSpec0) <- createUpdateSpecAction();
      instant1 = updateSpec0.creationTime.toEpochMilli + 1; // if created in quick succession duplicates creationTime
      (_, updateSpec1) <- createUpdateSpecFor(device.id, installPos = 0, withMillis = instant1)
      instant2 = updateSpec1.creationTime.toEpochMilli + 1; // if created in quick succession duplicates creationTime
      (_, updateSpec2) <- createUpdateSpecFor(device.id, installPos = 0, withMillis = instant2)
      result <- findPendingPackageIdsFor(device.id)
    } yield (result, updateSpec0, updateSpec1, updateSpec2)

    whenReady(db.run(dbIO)) { case (result, updateSpec0, updateSpec1, updateSpec2)  =>

      // fail install for update spec B only
      val device = updateSpec0.device
      val f2 = for {
        _    <- reportInstall(device, mkUpdateReport(updateSpec0, isSuccess = true))
        _    <- reportInstall(device, mkUpdateReport(updateSpec1, isSuccess = false))
        usRow0 <- db.run(UpdateSpecs.findBy(updateSpec0))
        usRow1 <- db.run(UpdateSpecs.findBy(updateSpec1))
        usRow2 <- db.run(UpdateSpecs.findBy(updateSpec2))
      } yield (usRow0, usRow1, usRow2)

      whenReady(f2) { case (usRow0, usRow1, usRow2) =>
        // check update spec 0 status finished
        // check update spec 1 status failed
        // check update spec 2 status canceled
        val (_, _, device0, status0, installPos0, _) = usRow0
        val (_, _, device1, status1, installPos1, _) = usRow1
        val (_, _, device2, status2, installPos2, _) = usRow2

        status0 shouldBe UpdateStatus.Finished
        status1 shouldBe UpdateStatus.Failed
        status2 shouldBe UpdateStatus.Pending
      }

    }
  }

}
