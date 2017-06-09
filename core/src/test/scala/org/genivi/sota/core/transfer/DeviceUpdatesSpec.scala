package org.genivi.sota.core.transfer

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import org.genivi.sota.DefaultPatience
import org.genivi.sota.core._
import org.genivi.sota.core.data.UpdateSpec
import org.genivi.sota.core.db._
import org.genivi.sota.core.rvi.OperationResult
import org.genivi.sota.core.rvi.UpdateReport
import org.genivi.sota.data.{DeviceGenerators, Namespaces, UpdateStatus, Uuid}
import org.genivi.sota.messaging.MessageBusPublisher
import org.scalacheck.Gen
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext
import slick.jdbc.MySQLProfile.api._


class DeviceUpdatesSpec extends FunSuite
  with ShouldMatchers
  with BeforeAndAfterAll
  with Inspectors
  with ScalaFutures
  with DatabaseSpec
  with DefaultPatience
  with UpdateResourcesDatabaseSpec {

  import DeviceGenerators._
  import DeviceUpdates._
  import Generators._

  implicit val actorSystem = ActorSystem("InstalledPackagesUpdateSpec-ActorSystem")
  implicit val materializer = ActorMaterializer()
  val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)
  val id = Uuid(Refined.unsafeApply(UUID.randomUUID().toString))

  implicit val ec = ExecutionContext.global

  test("forwards request to resolver client") {
    val device = genDeviceT.sample.get.copy(deviceId = Some(genDeviceId.sample.get))
    whenReady(deviceRegistry.createDevice(device)) { id =>
      val resolverClient = new FakeExternalResolver
      val packageIds = Gen.listOf(PackageIdGen).sample.get
      val f = update(id, packageIds, deviceRegistry)

      whenReady(f) { _ =>
        forAll(packageIds) { p =>
          deviceRegistry.isInstalled(id, p) shouldBe true
        }
      }
    }
  }

  test("marks reported packages as installed") {
    val f = for {
      (_, device, updateSpec) <- createUpdateSpec()
      result = OperationResult(id, 1, "some result")
      report = UpdateReport(updateSpec.request.id, List(result))
      _ <- reportInstall(device.uuid, report, MessageBusPublisher.ignore)
      updatedSpec <- db.run(findUpdateSpecFor(device.uuid, updateSpec.request.id))
      history <- db.run(InstallHistories.list(device.uuid))
    } yield (updatedSpec.status, history.map(_._1))

    whenReady(f) { case (newStatus, history) =>
      newStatus should be(UpdateStatus.Finished)
      history.map(_.success) should contain(true)
    }
  }

  test("when multiple packages are pending sorted by installPos") {
    val dbIO = for {
      (_, device, updateSpec0) <- createUpdateSpecAction()
      (_, updateSpec1) <- createUpdateSpecFor(device.uuid, installPos = 2)
      result <- findPendingPackageIdsFor(device.uuid)
    } yield (result.map(_._1), updateSpec0, updateSpec1)

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
      (_, spec1) <- createUpdateSpecFor(device.uuid)
      _ <- persistInstallOrder(device.uuid, List(spec0.request.id, spec1.request.id))
      dbSpecs <- findPendingPackageIdsFor(device.uuid)
    } yield (dbSpecs.map(_._1), spec0, spec1)

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
      _ <- updateSpecs.filter(_.device === device.uuid).map(_.status).update(UpdateStatus.InFlight)
      (_, spec1) <- createUpdateSpecFor(device.uuid)
      result <- persistInstallOrder(device.uuid, List(spec0.request.id, spec1.request.id))
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
      (_, spec1) <- createUpdateSpecFor(d.uuid)
      result <- persistInstallOrder(d.uuid, List(spec1.request.id))
    } yield result

    val f = db.run(dbIO)

    whenReady(f.failed) { t =>
      t shouldBe a[SetOrderFailed]
      t.getMessage should include("need to be specified")
    }
  }

  private def mkUpdateReport(updateSpec: UpdateSpec, isSuccess: Boolean): UpdateReport = {
    val result_code = if (isSuccess) 0 else 3
    val result = OperationResult(id, result_code, "some result")
    UpdateReport(updateSpec.request.id, List(result))
  }

  test("rest of installation queue (consecutive installPos) remains Pending upon one package failing to install") {

    // insert update spec A install pos 0
    // insert update spec B install pos 1
    // insert update spec C install pos 2
    val dbIO = for {
      (_, device, updateSpec0) <- createUpdateSpecAction()
      (_, updateSpec1) <- createUpdateSpecFor(device.uuid, installPos = 1)
      (_, updateSpec2) <- createUpdateSpecFor(device.uuid, installPos = 2)
      result <- findPendingPackageIdsFor(device.uuid)
    } yield (result.map(_._1), updateSpec0, updateSpec1, updateSpec2)

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
        _      <- reportInstall(device, mkUpdateReport(updateSpec0, isSuccess = true), MessageBusPublisher.ignore)
        _      <- reportInstall(device, mkUpdateReport(updateSpec1, isSuccess = false), MessageBusPublisher.ignore)
        usRow0 <- db.run(UpdateSpecs.findBy(updateSpec0))
        usRow1 <- db.run(UpdateSpecs.findBy(updateSpec1))
        usRow2 <- db.run(UpdateSpecs.findBy(updateSpec2))
      } yield (usRow0, usRow1, usRow2)

      whenReady(f2) { case (usRow0, usRow1, usRow2) =>
        // check update spec 0 status finished
        // check update spec 1 status failed
        // check update spec 2 status canceled
        usRow0.status shouldBe UpdateStatus.Finished
        usRow1.status shouldBe UpdateStatus.Failed
        usRow2.status shouldBe UpdateStatus.Pending
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
      (_, updateSpec1) <- createUpdateSpecFor(device.uuid, installPos = 0, withMillis = instant1)
      instant2 = updateSpec1.creationTime.toEpochMilli + 1; // if created in quick succession duplicates creationTime
      (_, updateSpec2) <- createUpdateSpecFor(device.uuid, installPos = 0, withMillis = instant2)
      result <- findPendingPackageIdsFor(device.uuid)
    } yield (result, updateSpec0, updateSpec1, updateSpec2)

    whenReady(db.run(dbIO)) { case (result, updateSpec0, updateSpec1, updateSpec2)  =>

      // fail install for update spec B only
      val device = updateSpec0.device
      val f2 = for {
        _    <- reportInstall(device, mkUpdateReport(updateSpec0, isSuccess = true), MessageBusPublisher.ignore)
        _    <- reportInstall(device, mkUpdateReport(updateSpec1, isSuccess = false), MessageBusPublisher.ignore)
        usRow0 <- db.run(UpdateSpecs.findBy(updateSpec0))
        usRow1 <- db.run(UpdateSpecs.findBy(updateSpec1))
        usRow2 <- db.run(UpdateSpecs.findBy(updateSpec2))
      } yield (usRow0, usRow1, usRow2)

      whenReady(f2) { case (usRow0, usRow1, usRow2) =>
        // check update spec 0 status finished
        // check update spec 1 status failed
        // check update spec 2 status canceled
        usRow0.status shouldBe UpdateStatus.Finished
        usRow1.status shouldBe UpdateStatus.Failed
        usRow2.status shouldBe UpdateStatus.Pending
      }

    }
  }

}
