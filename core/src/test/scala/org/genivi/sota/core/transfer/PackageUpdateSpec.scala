package org.genivi.sota.core.transfer

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import eu.timepit.refined.Refined
import org.genivi.sota.core.{PackagesReader, RequiresRvi, TestDatabase, UpdateService}
import org.genivi.sota.core.Generators
import org.genivi.sota.core.Generators.updateRequestGen
import org.genivi.sota.core.data.{Package, UpdateRequest, UpdateSpec, Vehicle}
import org.genivi.sota.core.db.Packages
import org.genivi.sota.core.jsonrpc.HttpTransport
import org.genivi.sota.core.rvi.{JsonRpcRviClient, SotaServices}
import org.genivi.sota.core.db.Vehicles
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}
import org.scalatest.concurrent.ScalaFutures.{PatienceConfig, convertScalaFuture, whenReady}
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}
import scala.concurrent.{Await, ExecutionContext, Future}
import slick.jdbc.JdbcBackend.Database

object DataGenerators {

  val packages = scala.util.Random.shuffle( PackagesReader.read().take(100).map(Generators.generatePackageData ) )

  def dependenciesGen(vin: Vehicle.IdentificationNumber) : Gen[UpdateService.VinsToPackages] = for {
    n                <- Gen.choose(1, 10)
    requiredPackages <- Gen.containerOfN[Set, Package.Id](n, Gen.oneOf(packages ).map( _.id ))
  } yield Map( vin -> requiredPackages )


  def updateWithDependencies(vin: Vehicle.IdentificationNumber) : Gen[(UpdateRequest, UpdateService.VinsToPackages)] = for {
    request      <- updateRequestGen(Gen.oneOf( packages.map( _.id) ))
    dependencies <- dependenciesGen( vin )
  } yield (request, dependencies)

  def requestsGen(vin: Vehicle.IdentificationNumber): Gen[Map[UpdateRequest, UpdateService.VinsToPackages]] = for {
    n        <- Gen.choose(1, 3)
    requests <- Gen.listOfN(n, updateWithDependencies(vin)).map( _.toMap )
  } yield requests

}

case class RviParameters[T](parameters: List[T], service_name: String  )

object SotaClient {
  import akka.actor.{Actor, ActorLogging, Props}
  import org.genivi.sota.core.rvi.{ClientServices, ServerServices, StartDownload, StartDownloadMessage, RviClient, JsonRpcRviClient}
  import io.circe._
  import io.circe.generic.auto._

  class ClientActor(rviClient: RviClient, clientServices: ClientServices) extends Actor with ActorLogging {
    def ttl() : DateTime = {
      import com.github.nscala_time.time.Implicits._
      DateTime.now + 5.minutes
    }

    def downloading( services: ServerServices ) : Receive = {
      case StartDownloadMessage(id, checkSum, size) =>

    }

    override def receive = {
      case UpdateNotification(packages, services ) =>
        log.debug( "Update notification received." )
        rviClient.sendMessage(services.start, StartDownload(Refined("VINOOLAM0FAU2DEEP"), packages.map(_.`package`).toList, clientServices), ttl())
      case m => log.debug(s"Not supported yet: $m")
    }
  }

  import akka.http.scaladsl.server._
  import akka.http.scaladsl.server.Directives._
  import org.genivi.sota.core.jsonrpc.JsonRpcDirectives._

  def forwardMessage[T](actor: ActorRef)(msg: RviParameters[T]) : Future[Unit] = {
    actor ! msg.parameters.head
    FastFuture.successful(())
  }

  def buildRoute(baseUri: Uri)
                (implicit system: ActorSystem, mat: ActorMaterializer) : Future[Route] = {
    import system.dispatcher
    import io.circe.generic.auto._
    val rviUri = Uri("http://127.0.0.1:8901") 
    implicit val clientTransport = HttpTransport( rviUri ).requestTransport
    val rviClient = new JsonRpcRviClient( clientTransport, system.dispatcher )

    def route(actorRef : ActorRef) = pathPrefix("sota" / "client") {
      path("notify") {
        service("message" -> lift[RviParameters[UpdateNotification], Unit](forwardMessage(actorRef)))
      }
    }

    registerServices(baseUri)(clientTransport, system.dispatcher).map( x =>
      system.actorOf(Props( new ClientActor(rviClient, x) ), "sota-client")
    ).map( route )
  }

  import org.genivi.sota.core.jsonrpc.client

  private[this] def registerService(name: String, uri: Uri)
                                   (implicit transport: Json => Future[Json], ec : ExecutionContext): Future[String] = {
    import shapeless._
    import record._
    import syntax.singleton._

    implicit val uriEncoder : Encoder[Uri] = Encoder[String].contramap[Uri]( _.toString() )

    client.register_service.request( ('service ->> name) :: ('network_address ->> uri) :: HNil, 1 ).run[Record.`'service -> String`.T]( transport ).map( _.get('service) )
  }

  def registerServices(baseUri: Uri)
              (implicit transport: Json => Future[Json], ec : ExecutionContext) : Future[ClientServices] = {
    val startRegistration = registerService("/sota/startdownload", baseUri.withPath( baseUri.path / "start"))
    val chunkRegistration = registerService("/sota/chunk", baseUri.withPath( baseUri.path / "chunk"))
    val finishRegistration = registerService("/sota/finish", baseUri.withPath( baseUri.path / "finish"))
    val notifyRegistration = registerService("/sota/notify", baseUri.withPath( baseUri.path / "notify"))
    for {
      startName  <- startRegistration
      chunkName  <- chunkRegistration
      finishName <- finishRegistration
      _          <- notifyRegistration
    } yield ClientServices( startName, chunkName, finishName )
  }

}

trait  SotaCore {
  import org.genivi.sota.core.rvi.{TransferProtocolActor, PackageTransferActor, UpdateController, JsonRpcRviClient}

  implicit val system = akka.actor.ActorSystem("PackageUpdateSpec")
  implicit val materilizer = akka.stream.ActorMaterializer()
  implicit val exec = system.dispatcher

  val databaseName = "test-database"
  implicit val db = Database.forConfig(databaseName)

  val updateService = new UpdateService()( akka.event.Logging(system, "sota.core.updateQueue") )

  val rviUri = Uri(system.settings.config.getString( "rvi.endpoint" ))
  implicit val serverTransport = HttpTransport( rviUri ).requestTransport

  implicit val rviClient = new JsonRpcRviClient( serverTransport, system.dispatcher )

  def sotaRviServices() : Route = {
    val transferProtocolProps = TransferProtocolActor.props(db, PackageTransferActor.props( rviClient ))
    val updateController = system.actorOf( UpdateController.props(transferProtocolProps ), "update-controller")
    new SotaServices(updateController).route
  } 

}

class PackageUpdateSpec extends PropSpec with PropertyChecks with Matchers with BeforeAndAfterAll with SotaCore {
  import DataGenerators._

  override def beforeAll() : Unit = {
    TestDatabase.resetDatabase( databaseName )
    import scala.concurrent.duration.DurationInt
    Await.ready( Future.sequence( packages.map( p => db.run( Packages.create(p) ) )), 50.seconds)
  }

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
  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 1)
  import org.scalacheck.Shrink
  implicit val noShrink: Shrink[Map[UpdateRequest, UpdateService.VinsToPackages]] = Shrink.shrinkAny

  import org.genivi.sota.core.rvi.UpdateEvents
  import scala.concurrent.duration.DurationInt

  def bindServices(serviceUri: Uri, startClient: Boolean = true) : Future[java.net.InetSocketAddress] = {
    import akka.http.scaladsl.server.Directives._
    for {
      clientRoute: Option[Route] <- if(startClient) SotaClient.buildRoute(serviceUri.withPath(Uri.Path / "sota" / "client")).map(Some.apply) else FastFuture.successful(None)
      route: Route = clientRoute.map(_ ~ sotaRviServices()).getOrElse(sotaRviServices())
      binding                    <- Http().bindAndHandle( route, serviceUri.authority.host.address(), serviceUri.authority.port )
    } yield binding.localAddress
  } 
    
  property("updates should be transfered to device", RequiresRvi) {
    forAll( requestsGen(Refined("VINOOLAM0FAU2DEEP")) ) { (requests) =>
      val probe = TestProbe()
      val serviceUri = Uri.from( scheme="http", host="192.168.1.64", port= 8080 )
      system.eventStream.subscribe(probe.ref, classOf[UpdateEvents.PackagesTransferred])
      val resultFuture = for {
        address        <- bindServices(serviceUri, startClient = true)
        serverServices <- SotaServices.register( serviceUri.withPath( Uri.Path / "rvi") )
        updateSpecs    <- init( requests )
        result         <- Future.sequence(UpdateNotifier.notify(updateSpecs.toSeq, serverServices))
      } yield result
      resultFuture.isReadyWithin( Span( 5, Seconds ) )
      probe.expectMsgType[UpdateEvents.PackagesTransferred](2.minutes)
    }
  }

  override def afterAll(): Unit = {
    db.close()
    TestKit.shutdownActorSystem(system)
  }
}
