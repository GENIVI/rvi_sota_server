/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core.rvi

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.core.data.{Package, Vehicle}
import io.circe.Json
import scala.concurrent.{ExecutionContext, Future}
import org.genivi.sota.core.ExternalResolverClient

final case class ServerServices(start: String, cancel: String, ack: String, report: String, packages: String)

final case class ClientServices( start: String, chunk: String, finish: String, getpackages: String )

final case class StartDownload(vin: Vehicle.Vin, packages: List[Package.Id], services: ClientServices )

final case class RviParameters[T](parameters: List[T], service_name: String )

final case class InstallReport(vin: Vehicle.Vin, `package`: Package.Id, status: Boolean,
                               description: String)

final case class InstalledPackages(vin: Vehicle.Vin, packages: Json )

class SotaServices(updateController: ActorRef, resolverClient: ExternalResolverClient)
                  (implicit system: ActorSystem, mat: ActorMaterializer) {
  import Directives._
  import org.genivi.sota.core.jsonrpc.JsonRpcDirectives._
  import system.dispatcher

  import io.circe.generic.auto._

  val log = Logging( system, "org.genivi.sota.core.SotaServices" )

  def forwardMessage[T](actor: ActorRef)(msg: RviParameters[T]) : Future[Unit] = {
    val payload = msg.parameters.head
    log.debug( s"Message from rvi: $payload" )
    actor ! payload
    FastFuture.successful(())
  }

  val route = pathPrefix("rvi") {
    path("start") {
      service( "message" -> lift[RviParameters[StartDownload], Unit](forwardMessage(updateController)) )
    } ~
    path("ack") {
      service( "message" -> lift[RviParameters[ChunksReceived], Unit](forwardMessage(updateController)))
    } ~
    path("report") {
      service( "message" -> lift[RviParameters[InstallReport], Unit](forwardMessage(updateController)))
    } ~
    path("packages") {
      service( "message" -> lift[RviParameters[InstalledPackages], Unit](forwardMessage(updateController)))
    }
  }

  def updatePackagesInResolver( message: InstalledPackages ) : Future[Unit] = {
    resolverClient.setInstalledPackages(message.vin, message.packages)
  }

}

object SotaServices {
  import io.circe._
  import org.genivi.sota.core.jsonrpc.client

  private[this] def registerService(name: String, uri: Uri)
    (implicit transport: Json => Future[Json], ec : ExecutionContext): Future[String] = {
    import shapeless._
    import record._
    import syntax.singleton._
    import io.circe._
    import io.circe.generic.auto._

    implicit val uriEncoder : Encoder[Uri] = Encoder[String].contramap[Uri]( _.toString() )

    client.register_service.request( ('service ->> name) :: ('network_address ->> uri) :: HNil, 1 )
      .run[Record.`'service -> String`.T]( transport ).map( _.get('service) )
  }

  def register(baseUri: Uri)
              (implicit transport: Json => Future[Json], ec : ExecutionContext) : Future[ServerServices] = {
    val startRegistration = registerService("/sota/start", baseUri.withPath( baseUri.path / "start"))
    val cancelRegistration = registerService("/sota/cancel", baseUri.withPath( baseUri.path / "cancel"))
    val ackRegistration = registerService("/sota/ack", baseUri.withPath( baseUri.path / "ack"))
    val reportRegistration = registerService("/sota/report", baseUri.withPath( baseUri.path / "report"))
    val packagesRegistration = registerService("/sota/packages", baseUri.withPath( baseUri.path / "packages"))
    for {
      startName  <- startRegistration
      cancelName <- cancelRegistration
      ackName    <- ackRegistration
      reportName <- reportRegistration
      packagesName <- packagesRegistration
    } yield ServerServices( start = startName, cancel = cancelName, ack = ackName,
                            report = reportName, packages = packagesName )
  }

}
