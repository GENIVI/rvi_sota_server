/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.resolver

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.{HttpEncoding, HttpEncodings}
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import org.genivi.sota.data.{Device, Namespace, PackageId}
import Device._
import cats.syntax.show._
import org.genivi.sota.marshalling.CirceMarshallingSupport

import scala.concurrent.Future

trait ExternalResolverClient {

  /**
    * Add a new package to SOTA
    * This is called when the user adds a new package to the SOTA system
    *
    * @param packageId The name and version of the package
    * @param description A description of the package (optional)
    * @param vendor The vendor providing the package (optional)
    */
  def putPackage(namespace: Namespace, packageId: PackageId, description: Option[String],
                 vendor: Option[String]): Future[Unit]

  /**
    * Given a package name and version, return vehicles that it should be
    * installed on.
    *
    * @param packageId The name and version of the package
    * @return Which packages need to be installed on which vehicles
    */
  def resolve(namespace: Namespace, packageId: PackageId): Future[Map[Device.Id, Set[PackageId]]]

  /**
    * Update the list of packages that are installed on a vehicle.
    * During normal operation SOTA will keep track of the state of of the
    * clients that are in the field. However there may be cases where this gets
    * out of sync for example if a ECU is replaced in the field, or when
    * packages are loaded in the factory.
    * The client can query the local package manager for the list of installed
    * packages and report it via this function
    *
    * @param device The device uuid that is sending the update
    * @param json A JSON encoded list of installed packages
    */
  def setInstalledPackages(device: Device.Id, json: io.circe.Json) : Future[Unit]
}

/**
 * A wrapper for an error that is thrown when trying to access the External
 * Resolver
 *
 * @param msg A message from the component that caught the exception
 * @param cause The underlying error that caused the request to fail
 */
case class ExternalResolverRequestFailed private ( msg: String, cause: Throwable ) extends Throwable( msg, cause )

/**
 * Utility functions to create ExternalResolverRequestFailed errors
 */
object ExternalResolverRequestFailed {

  /**
   * The request failed, but not as the result of an exception
   *
   * @param message A message from the application
   * @return An ExternalResolverRequestFailed throwable (which should be thrown)
   */
  def apply( message: String ) : ExternalResolverRequestFailed =
    new ExternalResolverRequestFailed(message, null)

  /**
   * The external resolver returned an unexpected HTTP status code
   *
   * @param statusCode The HTTP status code that was received
   * @return An ExternalResolverRequestFailed throwable (which should be thrown)
   */
  def apply( statusCode: StatusCode ) : ExternalResolverRequestFailed =
    new ExternalResolverRequestFailed( s"Unexpected status code received from external resolver '$statusCode'", null)

  /**
   * The request failed because an exception was raised
   *
   * @param cause The underlying exception
   * @return An ExternalResolverRequestFailed throwable (which should be thrown)
   */
  def apply( cause: Throwable ) : ExternalResolverRequestFailed =
    new ExternalResolverRequestFailed( "Request to external resolver failed.", cause )
}

/**
 * An implementation of the External Resolver that talks via HTTP to the
 * external resolver in this project
 */
class DefaultExternalResolverClient(baseUri : Uri, resolveUri: Uri, packagesUri: Uri, vehiclesUri: Uri)
                                   (implicit system: ActorSystem, mat: ActorMaterializer)
    extends ExternalResolverClient {

  import CirceMarshallingSupport._
  import io.circe._
  import system.dispatcher
  import io.circe.generic.auto._
  import akka.http.scaladsl.model.headers._

  private[this] val log = Logging( system, "org.genivi.sota.externalResolverClient" )

  override def resolve(namespace: Namespace, packageId: PackageId): Future[Map[Device.Id, Set[PackageId]]] = {
    val resolvePath = resolveUri
      .withPath(resolveUri.path)
      .withQuery(Query(
        "namespace" -> namespace.get,
        "package_name" -> packageId.name.get,
        "package_version" -> packageId.version.get
      ))

    val httpRequest = HttpRequest(uri = resolvePath)
      .addHeader(`Accept-Encoding`(HttpEncodings.gzip))

    val requestF = Http().singleRequest(httpRequest)

    requestF flatMap { response =>
      val e = if (response.encoding == HttpEncodings.gzip) {
        response.entity.transformDataBytes(Gzip.decoderFlow)
      } else {
        response.entity
      }

      Unmarshal(e).to[Map[Device.Id, Set[PackageId]]]
    } recoverWith { case t =>
      log.error(t, "Request to resolver failed")
      Future.failed(t)
    }
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

  override def setInstalledPackages(device: Device.Id, json: io.circe.Json) : Future[Unit] = {
    import akka.http.scaladsl.client.RequestBuilding.Put
    import org.genivi.sota.rest.ErrorRepresentation

    val uri = vehiclesUri.withPath( vehiclesUri.path / device.show / "packages" )
    val futureResult = Http().singleRequest( Put(uri, json) ).flatMap {
      case HttpResponse( StatusCodes.NoContent, _, _, _ ) =>
        FastFuture.successful( () )

      case HttpResponse( StatusCodes.BadRequest, _, entity, _ ) =>
        Unmarshal(entity).to[ErrorRepresentation].flatMap( x =>
          FastFuture.failed( ExternalResolverRequestFailed(s"Error returned from external resolver: $x") ) )

      case HttpResponse( status, _, _, _ ) =>
        FastFuture.failed( ExternalResolverRequestFailed(status) )
    }
    futureResult.onFailure { case t => log.error( t, "Request to external resolver failed." ) }
    futureResult
  }

  override def putPackage(namespace: Namespace, packageId: PackageId, description: Option[String],
                          vendor: Option[String]): Future[Unit] = {
    import akka.http.scaladsl.client.RequestBuilding._
    import shapeless._
    import syntax.singleton._
    import io.circe.generic.auto._

    val payload =
      ('id ->> ('name ->> packageId.name.get :: 'version ->> packageId.version.get :: HNil)) ::
      ('description ->> description) ::
      ('vendor  ->> vendor) ::
      ('namespace  ->> namespace.get) ::
      HNil

    val request : HttpRequest =
      Put(packagesUri.withPath(packagesUri.path / packageId.name.get / packageId.version.get ), payload)
    handlePutResponse( Http().singleRequest( request ) )
  }

}
