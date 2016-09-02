/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.rvi

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import io.circe.Json
import java.util.UUID

import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.resolver.{Connectivity, ExternalResolverClient}
import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.data.Device
import org.genivi.sota.marshalling.CirceMarshallingSupport._

import scala.concurrent.{ExecutionContext, Future}


/**
 * RVI paths for server-side services.
 */
final case class ServerServices(start: String, ack: String, report: String, packages: String)

/**
 * RVI paths for client-side services.
 */
final case class ClientServices( start: String, abort: String, chunk: String, finish: String, getpackages: String )

/**
 * RVI message from client to initiate a package download.
 */
final case class StartDownload(device: Device.Id, update_id: UUID, services: ClientServices)

/**
 * RVI parameters of generic type for a specified service name.
 */
final case class RviParameters[T](parameters: List[T], service_name: String )

final case class OperationResult(id: Device.Id, result_code: Int, result_text: String) {
  def isSuccess: Boolean = (result_code == 0) || (result_code == 1)
  def isFail: Boolean = !isSuccess
}

/**
  * @param update_id id of the [[UpdateRequest]] this report describes
  */
final case class UpdateReport(update_id: UUID, operation_results: List[OperationResult]) {
  def isSuccess: Boolean = !operation_results.exists(_.isFail)
  def isFail: Boolean = !isSuccess
}

/**
 * RVI message from client to report installation of a downloaded package.
 */
final case class InstallReport(device: Device.Id, update_report: UpdateReport)

/**
 * RVI message from client to report all installed packages.
 */
final case class InstalledPackages(device: Device.Id, installed_software: Json )

/**
 * HTTP endpoints for receideviceg messages from the RVI node.
 *
 * @param updateController the actor to forward messages for processing
 * @param resolverClient the resolver to update when a vehicle sends its installed packages
 */
class SotaServices(updateController: ActorRef, resolverClient: ExternalResolverClient, deviceRegistry: DeviceRegistry)
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
      service( "message" -> lift[RviParameters[InstalledPackages], Unit](
        m => updatePackagesInResolver(m.parameters.head)))
    }
  }

  def updatePackagesInResolver( message: InstalledPackages ) : Future[Unit] = {
    log.debug( s"InstalledPackages from rvi: $message" )
    resolverClient.setInstalledPackages(message.device, message.installed_software)
  }
}

object SotaServices {
  import io.circe._
  import org.genivi.sota.core.jsonrpc.client

  private[this] def registerService(name: String, uri: Uri)
    (implicit connectivity: Connectivity, ec : ExecutionContext): Future[String] = {
    import shapeless._
    import record._
    import syntax.singleton._
    import io.circe._
    import io.circe.generic.auto._

    implicit val uriEncoder : Encoder[Uri] = Encoder[String].contramap[Uri]( _.toString() )

    client.register_service.request( ('service ->> name) :: ('network_address ->> uri) :: HNil, 1 )
      .run[Record.`'service -> String`.T](connectivity.transport).map( _.get('service))
  }

  /**
   * Register our services to the RVI node.
   *
   * @param baseUri the edge URI that we are listening on
   * @return a future of the RVI paths for our services
   */
  def register(baseUri: Uri)
              (implicit connectivity: Connectivity, ec : ExecutionContext) : Future[ServerServices] = {
    val startRegistration = registerService("/sota/start", baseUri.withPath( baseUri.path / "start"))
    val ackRegistration = registerService("/sota/ack", baseUri.withPath( baseUri.path / "ack"))
    val reportRegistration = registerService("/sota/report", baseUri.withPath( baseUri.path / "report"))
    val packagesRegistration = registerService("/sota/packages", baseUri.withPath( baseUri.path / "packages"))
    for {
      startName  <- startRegistration
      ackName    <- ackRegistration
      reportName <- reportRegistration
      packagesName <- packagesRegistration
    } yield ServerServices( start = startName, ack = ackName,
                            report = reportName, packages = packagesName )
  }

}
