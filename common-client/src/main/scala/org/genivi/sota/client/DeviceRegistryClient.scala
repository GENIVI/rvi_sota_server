/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.client

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import cats.syntax.show._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data._
import org.genivi.sota.device_registry.common.Errors
import org.genivi.sota.http.NamespaceDirectives.nsHeader
import org.genivi.sota.marshalling.CirceMarshallingSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag


class DeviceRegistryClient(baseUri: Uri, devicesUri: Uri, deviceGroupsUri: Uri, mydeviceUri: Uri, packagesUri: Uri)
                          (implicit system: ActorSystem, mat: ActorMaterializer)
    extends DeviceRegistry {

  import CirceMarshallingSupport._
  import Device._
  import HttpMethods._
  import StatusCodes._

  private val log = Logging(system, "org.genivi.sota.deviceRegistryClient")

  private val http = Http()

  // TODO: change return type to Future[PaginatedResult[Device]]]
  override def searchDevice(ns: Namespace, re: String Refined Regex)
                           (implicit ec: ExecutionContext): Future[Seq[Device]] = {

    val pr = execHttp[PaginatedResult[Device]](HttpRequest(uri = baseUri.withPath(devicesUri.path)
      .withQuery(Query("regex" -> re.value)))
      .withHeaders(nsHeader(ns)))
      .recover { case t =>
        log.error(t, "Could not contact device registry")
        PaginatedResult[Device](0, 0, 0, Seq.empty[Device])
      }

    pr.map(_.values)
  }

  override def fetchDevice(ns: Namespace, uuid: Uuid)
                          (implicit ec: ExecutionContext): Future[Device] =
    execHttp[Device](HttpRequest(uri = baseUri.withPath(devicesUri.path / uuid.show))
                       .withHeaders(nsHeader(ns)))

  override def fetchMyDevice(uuid: Uuid)
                            (implicit ec: ExecutionContext): Future[Device] =
    execHttp[Device](HttpRequest(uri = baseUri.withPath(mydeviceUri.path / uuid.show)))

  override def fetchDevicesInGroup(ns: Namespace, uuid: Uuid)
                                  (implicit ec: ExecutionContext): Future[PaginatedResult[Uuid]] = {
    val path = deviceGroupsUri.path / uuid.show / "devices"
    val query = Query("limit" -> 1000.toString)
    execHttp[PaginatedResult[Uuid]](HttpRequest(uri = baseUri.withPath(path).withQuery(query))
      .withHeaders(nsHeader(ns)))
  }

  override def fetchByDeviceId(ns: Namespace, deviceId: DeviceId)
                              (implicit ec: ExecutionContext): Future[Device] =
    execHttp[Seq[Device]](HttpRequest(uri = baseUri.withPath(devicesUri.path)
      .withQuery(Query("deviceId" -> deviceId.show)))
      .withHeaders(nsHeader(ns)))
      .flatMap {
        case d +: _ => FastFuture.successful(d)
        case _ => FastFuture.failed(Errors.MissingDevice)
      }

  override def updateSystemInfo(uuid: Uuid, json: Json)
                               (implicit ec: ExecutionContext): Future[NoContent] =
    execHttp[NoContent](HttpRequest(method = PUT, uri = baseUri.withPath(mydeviceUri.path / uuid.show / "system_info"),
      entity = HttpEntity(ContentTypes.`application/json`, json.noSpaces)))

  override def setInstalledPackages(device: Uuid, packages: Seq[PackageId])
                                   (implicit ec: ExecutionContext) : Future[NoContent] =
    execHttp[NoContent](HttpRequest(method = PUT, uri = baseUri.withPath(mydeviceUri.path / device.show / "packages"),
      entity = HttpEntity(ContentTypes.`application/json`, packages.asJson.noSpaces)))

  override def affectedDevices(ns: Namespace, packageIds: Set[PackageId])
                              (implicit ec: ExecutionContext): Future[Map[Uuid, Set[PackageId]]] = {
    execHttp[Map[Uuid, Set[PackageId]]](
      HttpRequest(
        method = POST,
        uri = baseUri.withPath(packagesUri.path / "affected"),
        entity = HttpEntity(ContentTypes.`application/json`, packageIds.asJson.noSpaces))
      .withHeaders(nsHeader(ns))
    )
  }

  private def execHttp[T](httpRequest: HttpRequest)
                         (implicit unmarshaller: Unmarshaller[ResponseEntity, T],
                          ec: ExecutionContext, ct: ClassTag[T]): Future[T] = {
    http.singleRequest(httpRequest).flatMap { response =>
      response.status match {
        case Conflict => FastFuture.failed(Errors.ConflictingDeviceId)
        case NotFound => FastFuture.failed(Errors.MissingDevice)
        case StatusCodes.NoContent if ct.runtimeClass == classOf[NoContent] =>
          FastFuture.successful(org.genivi.sota.client.NoContent()).asInstanceOf[Future[T]]
        case other if other.isSuccess() => unmarshaller(response.entity)
        case err => log.error(s"Got exception for request to ${httpRequest.uri}\n" +
          s"Error message: ${response.entity.toString}")
          FastFuture.failed(new Exception(err.toString))
      }
    }
  }
}
