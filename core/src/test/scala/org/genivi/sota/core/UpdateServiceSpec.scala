package org.genivi.sota.core

import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import eu.timepit.refined.api.Refined
import java.util.UUID
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.{Packages, UpdateSpecs}
import org.genivi.sota.core.resolver.DefaultConnectivity
import org.genivi.sota.core.transfer.{DefaultUpdateNotifier, DeviceUpdates}
import org.genivi.sota.data.{Device, DeviceT, Namespaces, PackageId, Vehicle, VehicleGenerators}
import org.genivi.sota.data.Namespace._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}
import scala.concurrent.{Await, Future}
import scala.util.Random


/**
 * Spec tests for Update service
 */
class UpdateServiceSpec extends PropSpec
  with PropertyChecks
  with Matchers
  with DatabaseSpec
  with BeforeAndAfterAll
  with Namespaces {

  import org.genivi.sota.data.DeviceGenerators._

  val packages = PackagesReader.read().take(1000)

  implicit val _db = db

  implicit val system = akka.actor.ActorSystem("UpdateServiceSpec")
  import system.dispatcher
  implicit val mat = ActorMaterializer()

  override def beforeAll() : Unit = {
    super.beforeAll()
    import scala.concurrent.duration.DurationInt
    Await.ready( Future.sequence( packages.map( p => db.run( Packages.create(p) ) )), 50.seconds)
  }

  import Generators._

  implicit val updateQueueLog = akka.event.Logging(system, "sota.core.updateQueue")
  implicit val connectivity = DefaultConnectivity
  val deviceRegistry = new FakeDeviceRegistry

  val service = new UpdateService(DefaultUpdateNotifier, deviceRegistry)

  import org.genivi.sota.core.data.UpdateRequest
  import org.scalatest.concurrent.ScalaFutures.{whenReady, PatienceConfig}

  val AvailablePackageIdGen = Gen.oneOf(packages).map( _.id )

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 20)

  property("decline if package not found") {
    forAll(updateRequestGen(defaultNs, PackageIdGen)) { (request: UpdateRequest) =>
      whenReady( service.queueUpdate( request, _ => FastFuture.successful( Map.empty ) ).failed ) { e =>
        e shouldBe PackagesNotFound( request.packageId )
      }
    }
  }

  property("decline if some of dependencies not found") {
    def vinDepGen(missingPackages: Seq[PackageId]) : Gen[(Vehicle.Vin, Set[PackageId])] = for {
      vin               <- VehicleGenerators.genVin
      m                 <- Gen.choose(1, 10)
      availablePackages <- Gen.pick(m, packages).map( _.map(_.id) )
      n                 <- Gen.choose(1, missingPackages.length)
      deps              <- Gen.pick(n, missingPackages).map( xs => Random.shuffle(availablePackages ++ xs).toSet )
    } yield vin -> deps

    val resolverGen : Gen[(Seq[PackageId], UpdateService.DependencyResolver)] = for {
      n                 <- Gen.choose(1, 10)
      missingPackages   <- Gen.listOfN(n, PackageIdGen).map( _.toSeq )
      m                 <- Gen.choose(1, 10)
      vinsToDeps        <- Gen.listOfN(m, vinDepGen(missingPackages)).map( _.toMap )
    } yield (missingPackages, (_: Package) => FastFuture.successful(vinsToDeps))

    forAll(updateRequestGen(defaultNs, AvailablePackageIdGen), resolverGen) { (request, resolverConf) =>
      val (missingPackages, resolver) = resolverConf
      whenReady(service.queueUpdate(request, resolver).failed.mapTo[PackagesNotFound]) { failure =>
        failure.packageIds.toSet.union(missingPackages.toSet) should contain theSameElementsAs missingPackages
      }
    }
  }

  def createVehicles(vins: Set[Vehicle.Vin]) : Future[Unit] = {
    Future.sequence(vins.map { vin =>
      deviceRegistry.createDevice(DeviceT(genDeviceName.sample.get, Some(Device.DeviceId(vin.get))))
    }.toSeq).map(_ => ())
  }

  property("upload spec per device") {
    import scala.concurrent.duration.DurationInt
    forAll( updateRequestGen(defaultNs, AvailablePackageIdGen), dependenciesGen(packages) ) { (request, deps) =>
      val req = UpdateRequest(UUID.randomUUID(), request.namespace, request.packageId, request.creationTime,
        request.periodOfValidity, request.priority, request.signature, request.description, request.requestConfirmation)
      whenReady(createVehicles(deps.keySet)
        .flatMap(_ => service.queueUpdate(req, _ => Future.successful(deps)))) { specs =>
        Await.result(db.run(UpdateSpecs.listUpdatesById(Refined.unsafeApply(req.id.toString))), 1.second)
        specs.size shouldBe deps.size
      }
    }
  }

  property("queue an update for a single device creates an update request") {
    val newDevice = genDevice.sample.get
    val newPackage = PackageGen.sample.get

    val dbSetup = for {
      packageM <- Packages.create(newPackage)
    } yield (newDevice, packageM)

    val f = for {
      (device, packageM) <- db.run(dbSetup)
      updateRequest <- service.queueDeviceUpdate(device.namespace, device.id, packageM.id)
      queuedPackages <- db.run(DeviceUpdates.findPendingPackageIdsFor(device.id))
    } yield (updateRequest, queuedPackages)

    whenReady(f) { case (updateRequest, queuedPackages) =>
      updateRequest.packageId shouldBe newPackage.id
      queuedPackages.map(_.packageId) should contain(newPackage.id)
    }
  }

  override def afterAll() : Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }
}
