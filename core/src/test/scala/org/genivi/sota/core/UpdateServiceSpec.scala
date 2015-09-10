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

  def readPackage( src: Map[String, String] ) : Package = {
    if( src.get( "Package").isEmpty ) println( src )
    val maybePackage = for {
      name        <- src.get( "Package" )
      version     <- src.get( "Version" )
      size        <- src.get("Size").map( _.toLong )
      checkSum    <- src.get("SHA1")
    } yield Package( PackageId( Refined(name), Refined(version)), size = size, description = src.get( "Description" ), checkSum = checkSum, uri = Uri.Empty, vendor = src.get( "Maintainer" ) )
    maybePackage.get
  }

  val databaseName = "test-database"

  implicit val db = Database.forConfig(databaseName)

  val packages = readPackages().take(10)

  import scala.concurrent.ExecutionContext.Implicits.global

  override def beforeAll() : Unit = {
    TestDatabase.resetDatabase( databaseName )
    import scala.concurrent.duration.DurationInt
    Await.ready( Future.sequence( packages.map( p => db.run( Packages.create(p) ) )), 50.seconds)
  }

  def readPackages() = {
    val src = scala.io.Source.fromInputStream( this.getClass().getResourceAsStream("/Packages") )
    src.getLines().foldLeft( List(Map.empty[String, String]) ){ (acc, str) =>
      if( str.startsWith(" ") ) acc
      else {
        if( str.isEmpty ) Map.empty[String, String] :: acc
        else {
          val keyValue = str.split(": ").toList
          acc.head.updated(keyValue.head, keyValue.tail.mkString(": ")) :: acc.tail
        }
      }
    }.filter(_.nonEmpty).map(readPackage)
  }

  import Generators._

  val system = akka.actor.ActorSystem("UpdateServiseSpec")

  implicit val updateQueueLog = akka.event.Logging(system, "sota.core.updateQueue")

  val service = new UpdateService()

  import org.genivi.sota.core.data.UpdateRequest
  import com.github.nscala_time.time.Imports._
  import org.scalatest.concurrent.ScalaFutures.{whenReady, PatienceConfig}

  def updateRequestGen(packageIdGen : Gen[PackageId]) : Gen[UpdateRequest] = for {
    packageId    <- packageIdGen
    startAfter   <- Gen.choose(10, 100).map( DateTime.now + _.days)
    finishBefore <- Gen.choose(10, 100).map(x => startAfter + x.days)
    prio         <- Gen.choose(1, 10)
  } yield UpdateRequest( UUID.randomUUID(), packageId, DateTime.now, startAfter to finishBefore, prio )

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

  property("upload spec covers all required packages") {
    val vinDepGen : Gen[(Vehicle.IdentificationNumber, Set[PackageId])] = for {
      vin               <- vehicleGen.map( _.vin )
      m                 <- Gen.choose(1, 10)
      packages          <- Gen.pick(m, packages).map( _.map(_.id) )
    } yield vin -> packages.toSet

    val DependenciesGen : Gen[UpdateService.VinsToPackages] = for {
      n <- Gen.choose(1, 10)
      r <- Gen.listOfN(n, vinDepGen)
    } yield r.toMap

    forAll( updateRequestGen(AvailablePackageIdGen), DependenciesGen ) { (request, deps) =>
      val vehicles : Set[Vehicle] = deps.keySet.map( Vehicle.apply )
      import slick.driver.MySQLDriver.api._
      val vehiclesCreated : Future[Unit] = db.run( DBIO.seq( vehicles.map( Vehicles.create).toArray: _* ) )

      whenReady( vehiclesCreated.flatMap( _ => service.queueUpdate(request, _ => Future.successful(deps))) ) { specs =>
        val requestedPackages : Set[PackageId] = deps.values.fold(Set.empty[PackageId])( (acc, x) => acc.union(x) )
        val queuedPackages : Set[PackageId] = specs.map( spec => spec.downloads.map(_.packages.map(_.id).toSet)
          .fold(Set.empty[PackageId])( (acc, x) => acc.union(x )) )
          .fold(Set.empty[PackageId])( (acc, x) => acc.union(x ))
        requestedPackages should contain theSameElementsAs(queuedPackages)
      }
    }
  }

  override def afterAll() : Unit = {
    TestKit.shutdownActorSystem(system)
    db.close()
  }

}
