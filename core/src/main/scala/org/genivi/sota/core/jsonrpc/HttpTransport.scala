/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.jsonrpc

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.circe.Json
import org.genivi.sota.marshalling.CirceMarshallingSupport
import CirceMarshallingSupport._
import scala.concurrent.ExecutionContext

import scala.concurrent.Future

/**
 * HTTP transport to communicate with RVI node.
 *
 * @param rviUri the RVI node address
 */
class HttpTransport( rviUri : Uri ) {

  implicit def requestTransport
    (implicit actorSystem: ActorSystem, mat: akka.stream.ActorMaterializer, ec: ExecutionContext)
      : Json => Future[Json] =
    json => Http().singleRequest( Post(rviUri, json) ~> logRequest(actorSystem.log) ).flatMap( handleRequestResult )

  def handleRequestResult(response: HttpResponse)
                         (implicit mat: akka.stream.ActorMaterializer, exec: ExecutionContext) : Future[Json] =
    response match {
      case HttpResponse( StatusCodes.OK | StatusCodes.Accepted | StatusCodes.Created, _, entity, _ ) =>
        Unmarshal(entity).to[Json]

      case HttpResponse( status, _, entity, _) =>
        Unmarshal(entity).to[Json].recoverWith {
          case _ =>  Future.failed( client.TransportException( s"Unexpected response status code: $status" ) )
        }
    }

  implicit def notifyTransport
    (implicit actorSystem: ActorSystem, mat: akka.stream.ActorMaterializer, ec: ExecutionContext)
      : Json => Future[Unit] =
    json => Http().singleRequest( Post(rviUri, json) ).flatMap( handleNotifyResult )

  def handleNotifyResult(response: HttpResponse )
                        (implicit mat: akka.stream.ActorMaterializer, exec: ExecutionContext) : Future[Unit] =
    response match {
      case HttpResponse( StatusCodes.OK | StatusCodes.Accepted | StatusCodes.NoContent, _, _, _ ) =>
        Future.successful(())

      case HttpResponse( status, _, _, _) =>
        Future.failed( client.TransportException( s"Unexpected response status code: $status" ) )
    }

}

object HttpTransport {

  def apply( rviUri: Uri ) : HttpTransport = new HttpTransport(rviUri)

}
