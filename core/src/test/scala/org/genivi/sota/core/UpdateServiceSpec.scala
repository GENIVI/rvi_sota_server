package org.genivi.sota.core

import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.{BlacklistedPackages, Packages, UpdateSpecs}
import org.genivi.sota.core.resolver.DefaultConnectivity
import org.genivi.sota.core.transfer.{DefaultUpdateNotifier, DeviceUpdates}
import org.genivi.sota.data._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.SpanSugar._
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}
import slick.driver.MySQLDriver.api._

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
  with ScalaFutures
  with Namespaces {

  import Arbitrary._
  import DeviceGenerators._
  import UuidGenerator._

  val packages = PackagesReader.read().take(1000)

  implicit val defaultPatience = PatienceConfig(5.seconds, 500.millis)

  implicit val system = akka.actor.ActorSystem("UpdateServiceSpec")
  import system.dispatcher
  implicit val mat = ActorMaterializer()

  override def beforeAll() : Unit = {
    super.beforeAll()
    val dbio = DBIO.sequence(packages.map(Packages.create)).transactionally
    Await.ready(db.run(dbio), 50.seconds)
  }

  import Generators._

  implicit val updateQueueLog = akka.event.Logging(system, "sota.core.updateQueue")
  implicit val connectivity = DefaultConnectivity
  val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)

  val service = new UpdateService(DefaultUpdateNotifier, deviceRegistry)

  import org.genivi.sota.core.data.UpdateRequest

  val availablePackageIdGen = Gen.oneOf(packages).map(_.uuid)

  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 20)

  property("decline if package not found") {
    forAll(updateRequestGen(Gen.uuid)) { (request: UpdateRequest) =>
      whenReady( service.queueUpdate(defaultNs, request, _ => FastFuture.successful( Map.empty ) ).failed ) { e =>
        e shouldBe SotaCoreErrors.MissingPackage
      }
    }
  }

  property("decline if some of dependencies not found") {
    def vinDepGen(missingPackages: Seq[PackageId]) : Gen[(Uuid, Set[PackageId])] = for {
      deviceId          <- arbitrary[Uuid]
      m                 <- Gen.choose(1, 10)
      availablePackages <- Gen.pick(m, packages).map( _.map(_.id) )
      n                 <- Gen.choose(1, missingPackages.length)
      deps              <- Gen.pick(n, missingPackages).map( xs => Random.shuffle(availablePackages ++ xs).toSet )
    } yield deviceId -> deps

    val resolverGen : Gen[(Seq[PackageId], UpdateService.DependencyResolver)] = for {
      n                 <- Gen.choose(1, 10)
      missingPackages   <- Gen.listOfN(n, PackageIdGen).map( _.toSeq )
      m                 <- Gen.choose(1, 10)
      vinsToDeps        <- Gen.listOfN(m, vinDepGen(missingPackages)).map( _.toMap )
    } yield (missingPackages, (_: Package) => FastFuture.successful(vinsToDeps))

    forAll(updateRequestGen(availablePackageIdGen), resolverGen) { (request, resolverConf) =>
      val (missingPackages, resolver) = resolverConf
      whenReady(service.queueUpdate(defaultNs, request, resolver).failed.mapTo[PackagesNotFound]) { failure =>
        failure.packageIds.toSet.union(missingPackages.toSet) should contain theSameElementsAs missingPackages
      }
    }
  }

  property("upload spec per device") {
    forAll(updateRequestGen(availablePackageIdGen), dependenciesGen(packages)) { (req, deps) =>
      val queueF = for {
        specs <- service.queueUpdate(defaultNs, req, _ => Future.successful(deps))
        _ <- db.run(UpdateSpecs.listUpdatesById(Uuid.fromJava(req.id)))
      } yield specs

      whenReady(queueF) { specs =>
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
      updateRequest <- service.queueDeviceUpdate(device.namespace, device.uuid, packageM.id).map(_._1)
      queuedPackages <- db.run(DeviceUpdates.findPendingPackageIdsFor(device.uuid))
    } yield (updateRequest, queuedPackages.map(_._1))

    whenReady(f) { case (updateRequest, queuedPackages) =>
      updateRequest.packageUuid shouldBe newPackage.uuid
      queuedPackages.map(_.packageUuid) should contain(newPackage.uuid)
    }
  }

  property("queuing an update for a blacklisted package fails") {
    val newPackage = PackageGen.sample.get
    val req = updateRequestGen(Gen.uuid).sample.get.copy(packageUuid = newPackage.uuid)

    val f = for {
      packageM <- db.run(Packages.create(newPackage))
      _ <- BlacklistedPackages.create(packageM.namespace, packageM.id)
      _ <- service.queueUpdate(defaultNs, req, _ => Future.successful(Map.empty))
    } yield packageM

    val e = f.failed.futureValue

    e shouldBe SotaCoreErrors.BlacklistedPackage
  }

  property("queuing a device update for a blacklisted package fails") {
    val device = genDevice.sample.get
    val newPackage = PackageGen.sample.get

    val packageF = for {
      packageM <- db.run(Packages.create(newPackage))
      _ <- BlacklistedPackages.create(packageM.namespace, packageM.id)
      _ <- service.queueDeviceUpdate(device.namespace, device.uuid, packageM.id)
    } yield packageM

    packageF.failed.futureValue shouldBe SotaCoreErrors.BlacklistedPackage
  }

  property("fails if dependencies include blacklisted package") {
    val newPackage = PackageGen.sample.get
    val dependency = PackageGen.sample.get
    val device = arbitrary[Uuid].sample.get
    val req = updateRequestGen(Gen.uuid).sample.get.copy(packageUuid = newPackage.uuid)
    val fakeDependency = Map(device -> Set(dependency.id))

    val f = for {
      packageM <- db.run(Packages.create(newPackage))
      _ <- db.run(Packages.create(dependency))
      _ <- BlacklistedPackages.create(dependency.namespace, dependency.id)
      _ <- service.queueUpdate(defaultNs, req, _ => Future.successful(fakeDependency))
    } yield packageM

    val throwableF = f.failed.futureValue

    throwableF shouldBe SotaCoreErrors.BlacklistedPackage
  }

  override def afterAll() : Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }
}
