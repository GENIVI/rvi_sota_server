/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core.rvi

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import org.genivi.sota.core.data.Package

import scala.concurrent.{Future, ExecutionContext}

class WebService(implicit mat: ActorMaterializer, exec: ExecutionContext) extends Directives {

  def route(deviceCommunication: DeviceCommunication): Route = pathPrefix("rvi") {
    complete(NoContent)
  }
}

final case class ServerServices(start: String, cancel: String, ack: String, report: String)

case class ClientServices( start: String, chunk: String, finish: String )

case class StartDownload(packages: List[Package.Id], services: ClientServices )

class SotaServices(implicit system: ActorSystem, mat: ActorMaterializer) {
  import Directives._
  import org.genivi.sota.core.jsonrpc.JsonRpcDirectives._
  import system.dispatcher

  import io.circe.generic.semiauto._
  //import org.genivi.sota.CirceSupport._

  def startTransfer(message: StartDownload) : Future[Unit] = {
    ???
  }

  implicit val startDownloadDecoder = deriveFor[StartDownload].decoder

  val route = path("rvi") {
    service( "start" -> lift(startTransfer) )
  }

}

object SotaServices {
  import org.genivi.sota.core.jsonrpc.client
  import io.circe._

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
