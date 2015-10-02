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
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.core.data.{Package, Vehicle}

import scala.concurrent.{ExecutionContext, Future}

final case class ServerServices(start: String, cancel: String, ack: String, report: String)

case class ClientServices( start: String, chunk: String, finish: String )

case class StartDownload(vin: Vehicle.IdentificationNumber, packages: List[Package.Id], services: ClientServices )

case class RviParameters[T](parameters: List[T], service_name: String )

class SotaServices(updateController: ActorRef)
                  (implicit system: ActorSystem, mat: ActorMaterializer) {
  import Directives._
  import org.genivi.sota.core.jsonrpc.JsonRpcDirectives._
  import system.dispatcher

  import io.circe.generic.auto._
  import CirceMarshallingSupport._

  val log = Logging( system, "sota.core.sotaService" )

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
    }
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

    client.register_service.request( ('service ->> name) :: ('network_address ->> uri) :: HNil, 1 ).run[Record.`'service -> String`.T]( transport ).map( _.get('service) )
  }

  def register(baseUri: Uri)
              (implicit transport: Json => Future[Json], ec : ExecutionContext) : Future[ServerServices] = {
    val startRegistration = registerService("/sota/start", baseUri.withPath( baseUri.path / "start"))
    val cancelRegistration = registerService("/sota/cancel", baseUri.withPath( baseUri.path / "cancel"))
    val ackRegistration = registerService("/sota/ack", baseUri.withPath( baseUri.path / "ack"))
    val reportRegistration = registerService("/sota/report", baseUri.withPath( baseUri.path / "report"))
    for {
      startName <- startRegistration
      cancelName <- cancelRegistration
      ackName <- ackRegistration
      reportName <- reportRegistration
    } yield ServerServices( start = startName, cancel = cancelName, ack = ackName, report = reportName )
  }

}