/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import org.genivi.sota.core.data.{PackageId, Vehicle}

import scala.concurrent.{ExecutionContext, Future}

trait ExternalResolverClient {

  def putPackage(packageId: PackageId, description: Option[String], vendor: Option[String]): Future[Unit]

  def resolve(packageId: PackageId): Future[Map[Vehicle, Set[PackageId]]]
}

case class ExternalResolverRequestFailed private ( msg: String, cause: Throwable ) extends Throwable( msg, cause )

object ExternalResolverRequestFailed {

  def apply( statusCode: StatusCode ) : ExternalResolverRequestFailed =
    new ExternalResolverRequestFailed( s"Unexpected status code received from external resolver '$statusCode'", null)

  def apply( cause: Throwable ) : ExternalResolverRequestFailed = new ExternalResolverRequestFailed( "Request to external resolver failed.", cause )
}

class DefaultExternalResolverClient(baseUri : Uri)
                                   (implicit system: ActorSystem, mat: ActorMaterializer) extends ExternalResolverClient {

  import system.dispatcher
  import io.circe._
  import io.circe.generic.auto._
  import org.genivi.sota.CirceSupport._

  //import org.genivi.sota.core.data.CodecInstances._

  private[this] val log = Logging( system, "externalResolverClient" )

  override def resolve(packageId: PackageId): Future[Map[Vehicle, Set[PackageId]]] = {
    implicit val responseDecoder : Decoder[Map[Vehicle.IdentificationNumber, Set[PackageId]]] =
      Decoder[Seq[(Vehicle.IdentificationNumber, Set[PackageId])]].map(_.toMap)


    request(packageId).flatMap { response =>
      Unmarshal(response.entity).to[Map[Vehicle.IdentificationNumber, Set[PackageId]]].map { parsed =>
        parsed.map { case (k, v) => Vehicle(k) -> v }
      }
    }.recover { case _ => Map.empty[Vehicle, Set[PackageId]] }
  }

  def handlePutResponse( futureResponse: Future[HttpResponse] ) : Future[Unit] =
    futureResponse.flatMap { response =>
      response.status match {
        case StatusCodes.OK => FastFuture.successful(())
        case sc =>
          log.warning( s"Unexpected response to put package request with status code '$sc'")
          Future.failed( ExternalResolverRequestFailed(sc) )
      }
    }.recoverWith {
      case e: ExternalResolverRequestFailed => Future.failed(e)
      case e => Future.failed( ExternalResolverRequestFailed(e) )
    }


  override def putPackage(packageId: PackageId, description: Option[String], vendor: Option[String]): Future[Unit] = {
    import akka.http.scaladsl.client.RequestBuilding._
    import shapeless._
    import record._
    import syntax.singleton._
    import io.circe.generic.auto._
    import io.circe.generic.semiauto._

    val payload =
      ('name ->> packageId.name.get) ::
      ('version ->> packageId.version.get) ::
      ('description ->> description) ::
      ('vendor  ->> vendor) ::
      HNil

    //implicit val payloadEncoder : Encoder[Record.`'name -> String, 'version -> String, 'description -> Option[String], 'vendor -> Option[String]`.T] = ???

    val request : HttpRequest = Put(baseUri.withPath( baseUri.path / packageId.name.get / packageId.version.get ), payload)
    handlePutResponse( Http().singleRequest( request ) )
  }

  private def request(packageId: PackageId): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = baseUri.withPath( baseUri.path / packageId.name.get / packageId.version.get)))
  }
}
