package org.genivi.sota.core.transfer

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import org.genivi.sota.core.Generators
import org.genivi.sota.core.Generators.updateRequestGen
import org.genivi.sota.core.data.{Package, UpdateRequest, UpdateSpec}
import org.genivi.sota.core.db.{Packages, Vehicles}
import org.genivi.sota.core.jsonrpc.HttpTransport
import org.genivi.sota.core.resolver.DefaultExternalResolverClient
import org.genivi.sota.core.rvi._
import org.genivi.sota.core.{PackagesReader, RequiresRvi, TestDatabase, UpdateService}
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{Namespaces, PackageId, Vehicle}
import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures.{PatienceConfig, convertScalaFuture, whenReady}
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}
import scala.concurrent.{Await, ExecutionContext, Future}
import slick.jdbc.JdbcBackend.Database


/**
 * Generator for spec data, including packages, dependencies and requests
 */
object DataGenerators {

  val packages = scala.util.Random.shuffle( PackagesReader.read().take(10).map(Generators.generatePackageData ) )

  def dependenciesGen(packageId: PackageId, vin: Vehicle.Vin) : Gen[UpdateService.VinsToPackages] =
    for {
      n                <- Gen.choose(1, 3)
      requiredPackages <- Gen.containerOfN[Set, PackageId](n, Gen.oneOf(packages ).map( _.id ))
    } yield Map( vin -> (requiredPackages + packageId) )


  def updateWithDependencies(vin: Vehicle.Vin) : Gen[(UpdateRequest, UpdateService.VinsToPackages)] =
    for {
      packageId    <- Gen.oneOf( packages.map( _.id) )
      request      <- updateRequestGen(Namespaces.defaultNs, packageId)
      dependencies <- dependenciesGen( packageId, vin )
    } yield (request, dependencies)

  def requestsGen(vin: Vehicle.Vin): Gen[Map[UpdateRequest, UpdateService.VinsToPackages]] = for {
    n        <- Gen.choose(1, 4)
    requests <- Gen.listOfN(1, updateWithDependencies(vin)).map( _.toMap )
  } yield requests

}

/**
 * Dummy SotaClient object for tests
 */
object SotaClient {
  import akka.actor.{Actor, ActorLogging, Props}
  import org.genivi.sota.core.rvi.{ClientServices, JsonRpcRviClient, RviParameters, ServerServices, StartDownload, StartDownloadMessage}
  import io.circe._
  import io.circe.generic.auto._
  import org.genivi.sota.core.resolver.ConnectivityClient
  import org.genivi.sota.marshalling.CirceInstances._

  class ClientActor(rviClient: ConnectivityClient, clientServices: ClientServices) extends Actor with ActorLogging {
    def ttl() : DateTime = {
      import com.github.nscala_time.time.Implicits._
      DateTime.now + 5.minutes
    }

    def downloading( services: ServerServices ) : Receive = {
      case StartDownloadMessage(id, checkSum, size) =>

    }

    val vin = refineV[Vehicle.ValidVin]("V1234567890123456").right.get

    override def receive = {
      case UpdateNotification(update, services ) =>
        log.debug( "Update notification received." )
        rviClient.sendMessage(
          services.start,
          StartDownload(vin, update.update_id, clientServices),
          ttl())
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

    val rviUri = Uri(system.settings.config.getString( "rvi.endpoint" ))
    implicit val clientTransport = HttpTransport( rviUri ).requestTransport
    val rviClient = new JsonRpcRviClient( clientTransport, system.dispatcher )

    import io.circe.generic.auto._
    import org.genivi.sota.marshalling.CirceInstances._
    import com.github.nscala_time.time.Imports._

    // TODO: Handle start,chunk,finish messages
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

    client.register_service.request( ('service ->> name) :: ('network_address ->> uri) :: HNil, 1 )
      .run[Record.`'service -> String`.T]( transport ).map( _.get('service) )
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
    } yield ClientServices( startName, "", chunkName, finishName, "" )
  }

}

/**
 * Generic trait for SOTA Core tests. Includes dummy RVI service routes.
 */
trait SotaCore {
  import org.genivi.sota.core.rvi.{TransferProtocolActor, PackageTransferActor,
    UpdateController, JsonRpcRviClient, RviConnectivity}

  implicit val system = akka.actor.ActorSystem("PackageUpdateSpec")
  implicit val materilizer = akka.stream.ActorMaterializer()
  implicit val exec = system.dispatcher

  val databaseName = "test-database"
  implicit val db = Database.forConfig(databaseName)

  implicit val connectivity = new RviConnectivity

  def sotaRviServices() : Route = {
    val transferProtocolProps =
      TransferProtocolActor.props(db, connectivity.client,
                                  PackageTransferActor.props(connectivity.client))
    val updateController = system.actorOf( UpdateController.props(transferProtocolProps ), "update-controller")
    val client = new DefaultExternalResolverClient( Uri.Empty, Uri.Empty, Uri.Empty, Uri.Empty )
    new SotaServices(updateController, client).route
  }

}

/**
 * Property Spec for testing package updates
 */
class PackageUpdateSpec extends PropSpec
  with PropertyChecks
  with Matchers
  with BeforeAndAfterAll
  with SotaCore
  with Namespaces {

  import DataGenerators._

  override def beforeAll() : Unit = {
    TestDatabase.resetDatabase( databaseName )
    import scala.concurrent.duration.DurationInt
    Await.ready( Future.sequence( packages.map( p => db.run( Packages.create(p) ) )), 50.seconds)
  }

  def init(services: ServerServices,
           generatedData: Map[UpdateRequest, UpdateService.VinsToPackages]): Future[Set[UpdateSpec]] = {
    import slick.driver.MySQLDriver.api._

    val notifier = new RviUpdateNotifier(services)
    val updateService = new UpdateService(notifier)(system, connectivity)
    val vins : Set[Vehicle.Vin] =
      generatedData.values.map( _.keySet ).fold(Set.empty[Vehicle.Vin])( _ union _)

    for {
      _     <- db.run( DBIO.seq( vins.map( vin => Vehicles.create(Vehicle(defaultNs, vin))).toArray: _* ) )
      specs <- Future.sequence( generatedData.map {
                                 case (request, deps) =>
                                   updateService.queueUpdate(request, _ => FastFuture.successful(deps))
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
      clientRoute: Option[Route] <- if(startClient) {
        SotaClient.buildRoute(serviceUri.withPath(Uri.Path / "sota" / "client")).map(Some.apply)
      } else FastFuture.successful(None)
      route: Route = clientRoute.map(_ ~ sotaRviServices()).getOrElse(sotaRviServices())
      binding      <- Http().bindAndHandle( route, serviceUri.authority.host.address(), serviceUri.authority.port )
    } yield binding.localAddress
  }

  implicit val log = Logging(system, "org.genivi.sota.core.PackageUpload")

  property("updates should be transfered to device", RequiresRvi) {
    forAll( requestsGen(Refined.unsafeApply("V1234567890123456")) ) { (requests) =>
      val probe = TestProbe()
      val serviceUri = Uri.from(scheme="http", host=getLocalHostAddr, port=8088)
      system.eventStream.subscribe(probe.ref, classOf[UpdateEvents.InstallReportReceived])
      val resultFuture = for {
        address        <- bindServices(serviceUri, startClient = true)
        serverServices <- SotaServices.register( serviceUri.withPath(Uri.Path / "rvi") )
        updateSpecs    <- init( serverServices, requests )
      } yield updateSpecs
      resultFuture.isReadyWithin( Span( 5, Seconds ) )
      probe.expectMsgType[UpdateEvents.InstallReportReceived](20.seconds)
    }
  }

  override def afterAll(): Unit = {
    db.close()
    TestKit.shutdownActorSystem(system)
  }

  def getLocalHostAddr = {
    import collection.JavaConversions._
    java.net.NetworkInterface.getNetworkInterfaces
      .flatMap(_.getInetAddresses.toSeq)
      .find(a => a.isSiteLocalAddress && !a.isLoopbackAddress)
      .getOrElse(java.net.InetAddress.getLocalHost)
      .getHostAddress
  }

}
