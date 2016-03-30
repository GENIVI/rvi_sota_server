package org.genivi.sota.core.transfer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.genivi.sota.core.db._

import org.genivi.sota.core.rvi.UpdateReport
import org.genivi.sota.core.rvi.OperationResult
import org.genivi.sota.data.VehicleGenerators

import org.genivi.sota.core.{UpdateResourcesDatabaseSpec, Generators, DatabaseSpec, FakeExternalResolver}
import org.genivi.sota.core.data.UpdateStatus

import org.scalacheck.Gen
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.genivi.sota.db.SlickExtensions
import org.scalatest.time.{Millis, Seconds, Span}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import scala.concurrent.ExecutionContext

class InstalledPackagesUpdateSpec extends FunSuite
  with ShouldMatchers
  with BeforeAndAfterAll
  with Inspectors
  with ScalaFutures
  with DatabaseSpec
  with UpdateResourcesDatabaseSpec {

  import Generators._
  import SlickExtensions._

  implicit val actorSystem = ActorSystem("InstalledPackagesUpdateSpec-ActorSystem")
  implicit val materializer = ActorMaterializer()

  implicit val ec = ExecutionContext.global
  implicit val _db = db
  implicit val patience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  test("forwards request to resolver client") {
    val resolverClient = new FakeExternalResolver
    val vin = VehicleGenerators.genVin.sample.get
    val packageIds = Gen.listOf(PackageIdGen).sample.get
    val f = InstalledPackagesUpdate.update(vin, packageIds, resolverClient)

    whenReady(f) { _ =>
      forAll(packageIds) { id =>
        resolverClient.installedPackages should contain(id)
      }
    }
  }

  test("marks reported packages as installed") {
    val f = for {
      (_, vehicle, updateSpec) <- createUpdateSpec()
      result = OperationResult("opid", 1, "some result")
      report = UpdateReport(updateSpec.request.id, List(result))
      _ <- InstalledPackagesUpdate.reportInstall(vehicle.vin, report)
      updatedSpec <- db.run(InstalledPackagesUpdate.findUpdateSpecFor(vehicle.vin, updateSpec.request.id))
      history <- db.run(InstallHistories.list(vehicle.namespace, vehicle.vin))
    } yield (updatedSpec.status, history)

    whenReady(f) { case (newStatus, history) =>
      newStatus should be(UpdateStatus.Finished)
      history.map(_.success) should contain(true)
    }
  }
}
