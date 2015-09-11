package org.genivi.sota.core.transfer

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.util.FastFuture
import akka.testkit.TestKit
import eu.timepit.refined.Refined
import org.genivi.sota.core.TestDatabase
import org.genivi.sota.core.UpdateService
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.data.PackageId
import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.core.data.UpdateSpec
import org.genivi.sota.core.data.Vehicle
import org.genivi.sota.core.db.Packages
import org.genivi.sota.core.db.Vehicles
import org.genivi.sota.core.jsonrpc.HttpTransport
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterAll
import org.scalatest.{PropSpec, Matchers}
import org.scalatest.prop.PropertyChecks
import org.genivi.sota.core.PackagesReader
import org.genivi.sota.core.RequiresRvi
import org.genivi.sota.core.Generators.updateRequestGen
import scala.concurrent.Await
import scala.concurrent.Future
import slick.jdbc.JdbcBackend.Database
import org.scalatest.time.{Millis, Seconds, Span}

class PackageUpdateSpec extends PropSpec with PropertyChecks with Matchers with BeforeAndAfterAll {

  val packages = scala.util.Random.shuffle( PackagesReader.read() ).take(1000)

  implicit val system = akka.actor.ActorSystem("PackageUpdateSpec")
  implicit val materilizer = akka.stream.ActorMaterializer()
  import system.dispatcher

  val databaseName = "test-database"
  implicit val db = Database.forConfig(databaseName)

  override def beforeAll() : Unit = {
    TestDatabase.resetDatabase( databaseName )
    import scala.concurrent.duration.DurationInt
    Await.ready( Future.sequence( packages.map( p => db.run( Packages.create(p) ) )), 50.seconds)
  }

  import org.scalatest.concurrent.ScalaFutures.{convertScalaFuture, whenReady, PatienceConfig}

  def dependenciesGen(vin: Vehicle.IdentificationNumber) : Gen[UpdateService.VinsToPackages] = for {
    requiredPackages <- Gen.nonEmptyContainerOf[Set, PackageId]( Gen.oneOf(packages ).map( _.id ))
  } yield Map( vin -> requiredPackages )


  def updateWithDependencies(vin: Vehicle.IdentificationNumber) : Gen[(UpdateRequest, UpdateService.VinsToPackages)] = for {
    request      <- updateRequestGen(Gen.oneOf( packages.map( _.id) ))
    dependencies <- dependenciesGen( vin )
  } yield (request, dependencies)

  def requestsGen(vin: Vehicle.IdentificationNumber): Gen[Map[UpdateRequest, UpdateService.VinsToPackages]] =
    Gen.nonEmptyListOf(updateWithDependencies(vin)).map( _.toMap )

  val updateService = new UpdateService()( akka.event.Logging(system, "sota.core.updateQueue") )

  def init( generatedData: Map[UpdateRequest, UpdateService.VinsToPackages] ) : Future[Set[UpdateSpec]] = {
    val vins : Set[Vehicle.IdentificationNumber] = generatedData.values.map( _.keySet ).fold(Set.empty[Vehicle.IdentificationNumber])( _ union _)
    import slick.driver.MySQLDriver.api._
    for {
      _     <- db.run( DBIO.seq( vins.map( vin => Vehicles.create(Vehicle(vin))).toArray: _* ) )
      specs <- Future.sequence( generatedData.map{
                case (request, deps) => updateService.queueUpdate(request, _ => FastFuture.successful(deps))
              })
    } yield specs.foldLeft(Set.empty[UpdateSpec])(_ union _)
    
  }

  implicit val patience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 10)
  import org.scalacheck.Shrink
  //implicit val noDepsShrink: Shrink[UpdateService.VinsToPackages] = Shrink.shrinkAny
  implicit val noShrink: Shrink[Map[UpdateRequest, UpdateService.VinsToPackages]] = Shrink.shrinkAny

  val rviUri = Uri(system.settings.config.getString( "rvi.endpoint" ))
  val serverTransport = HttpTransport( rviUri )
  import serverTransport.requestTransport

  property("updates should be transfered to device", RequiresRvi) {
    forAll( requestsGen(Refined("VINOOLAM0FAU2DEEP")) ) { (requests) =>
      val updateSpecs = init( requests ).futureValue
      Future.sequence(UpdateNotifier.notify(updateSpecs.toSeq)).isReadyWithin( Span( 5, Seconds ) )
    }
  }

  override def afterAll(): Unit = {
    db.close()
    TestKit.shutdownActorSystem(system)
  }
}
