package org.genivi.sota.core

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.util.FastFuture
import akka.testkit.TestKit
import eu.timepit.refined.Refined
import java.util.UUID
import org.genivi.sota.core.data.Vehicle
import org.genivi.sota.core.data.{PackageId, Package}
import org.genivi.sota.core.db.Packages
import org.genivi.sota.core.db.Vehicles
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalatest.time.Millis
import org.scalatest.time.Second
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfterAll, PropSpec, Matchers}
import org.scalatest.prop.PropertyChecks
import scala.util.Random
import slick.jdbc.JdbcBackend._

import scala.concurrent.{Await, Future}

class UpdateServiceSpec extends PropSpec with PropertyChecks with Matchers with BeforeAndAfterAll {

  val databaseName = "test-database"

  implicit val db = Database.forConfig(databaseName)

  val packages = PackagesReader.read().take(1000)

  val system = akka.actor.ActorSystem("UpdateServiseSpec")
  import system.dispatcher

  override def beforeAll() : Unit = {
    TestDatabase.resetDatabase( databaseName )
    import scala.concurrent.duration.DurationInt
    Await.ready( Future.sequence( packages.map( p => db.run( Packages.create(p) ) )), 50.seconds)
  }

  import Generators._


  implicit val updateQueueLog = akka.event.Logging(system, "sota.core.updateQueue")

  val service = new UpdateService()

  import org.genivi.sota.core.data.UpdateRequest
  import org.scalatest.concurrent.ScalaFutures.{whenReady, PatienceConfig}

  val AvailablePackageIdGen = Gen.oneOf(packages).map( _.id )

  implicit val defaultPatience = PatienceConfig(timeout = Span(1, Second), interval = Span(500, Millis))

  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 20)

  property("decline if package not found") {
    forAll(updateRequestGen(PackageIdGen)) { (request: UpdateRequest) =>
      whenReady( service.queueUpdate( request, _ => FastFuture.successful( Map.empty ) ).failed ) { e =>
        e shouldBe PackagesNotFound( request.packageId )
      }
    }
  }

  property("decline if some of dependencies not found") {
    def vinDepGen(missingPackages: Seq[PackageId]) : Gen[(Vehicle.IdentificationNumber, Set[PackageId])] = for {
      vin               <- vehicleGen.map( _.vin )
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

    forAll(updateRequestGen( AvailablePackageIdGen ), resolverGen) { (request, resolverConf) =>
      val (missingPackages, resolver) = resolverConf
      whenReady(service.queueUpdate(request, resolver).failed.mapTo[PackagesNotFound]) { failure =>
        failure.packageIds.toSet.union(missingPackages.toSet) should contain theSameElementsAs missingPackages
      }
    }
  }

  def createVehicles( vins: Set[Vehicle.IdentificationNumber] ) : Future[Unit] = {
    import slick.driver.MySQLDriver.api._
    db.run( DBIO.seq( vins.map( vin => Vehicles.create(Vehicle(vin))).toArray: _* ) )
  }

  property("upload spec per vin") {
    forAll( updateRequestGen(AvailablePackageIdGen), dependenciesGen(packages) ) { (request, deps) =>
      whenReady( createVehicles(deps.keySet).flatMap( _ => service.queueUpdate(request, _ => Future.successful(deps))) ) { specs =>
        specs.size shouldBe deps.size
      }
    }
  }

  override def afterAll() : Unit = {
    TestKit.shutdownActorSystem(system)
    db.close()
  }

}
