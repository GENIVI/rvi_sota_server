package org.genivi.sota.core

import java.util.UUID

import akka.http.scaladsl.util.FastFuture
import akka.testkit.TestKit
import eu.timepit.refined.api.Refined
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.{Packages, UpdateSpecs}
import org.genivi.sota.core.resolver.DefaultConnectivity
import org.genivi.sota.core.transfer.{DefaultUpdateNotifier, VehicleUpdates}
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.Namespaces
import org.genivi.sota.data.{PackageId, Vehicle, VehicleGenerators}
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

  val packages = PackagesReader.read().take(1000)

  implicit val _db = db

  implicit val system = akka.actor.ActorSystem("UpdateServiseSpec")
  import system.dispatcher

  override def beforeAll() : Unit = {
    super.beforeAll()
    import scala.concurrent.duration.DurationInt
    Await.ready( Future.sequence( packages.map( p => db.run( Packages.create(p) ) )), 50.seconds)
  }

  import Generators._

  implicit val updateQueueLog = akka.event.Logging(system, "sota.core.updateQueue")
  implicit val connectivity = DefaultConnectivity

  val service = new UpdateService(DefaultUpdateNotifier)

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

  def createVehicles(ns: Namespace, vins: Set[Vehicle.Vin]) : Future[Unit] = {
    // TODO
    FastFuture.successful(())
    // import slick.driver.MySQLDriver.api._
    // db.run(DBIO.seq(vins.map(vin => Vehicles.create(Vehicle(ns, vin))).toArray: _*))
  }

  property("upload spec per vin") {
    import scala.concurrent.duration.DurationInt
    forAll( updateRequestGen(defaultNs, AvailablePackageIdGen), dependenciesGen(packages) ) { (request, deps) =>
      val req = UpdateRequest(UUID.randomUUID(), request.namespace, request.packageId, request.creationTime,
        request.periodOfValidity, request.priority, request.signature, request.description, request.requestConfirmation)
      whenReady(createVehicles(req.namespace, deps.keySet)
        .flatMap(_ => service.queueUpdate(req, _ => Future.successful(deps)))) { specs =>
        Await.result(db.run(UpdateSpecs.listUpdatesById(Refined.unsafeApply(req.id.toString))), 1.second)
        specs.size shouldBe deps.size
      }
    }
  }

  property("queue an update for a single vehicle creates an update request") {
    val newVehicle = VehicleGenerators.genVehicle.sample.get
    val newPackage = PackageGen.sample.get

    val dbSetup = for {
      // vehicle <- Vehicles.create(newVehicle)
      packageM <- Packages.create(newPackage)
    } yield (newVehicle, packageM)

    val f = for {
      (vehicle, packageM) <- db.run(dbSetup)
      updateRequest <- service.queueVehicleUpdate(vehicle.namespace, vehicle.vin, packageM.id)
      queuedPackages <- db.run(VehicleUpdates.findPendingPackageIdsFor(vehicle.vin))
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
