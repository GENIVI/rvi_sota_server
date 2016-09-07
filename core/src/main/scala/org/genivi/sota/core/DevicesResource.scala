/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.time.Instant

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import io.circe.generic.auto._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.data._
import org.genivi.sota.core.resolver.{ConnectivityClient, ExternalResolverClient}
import org.genivi.sota.data.Device
import org.genivi.sota.data.Namespace
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.marshalling.RefinedMarshallingSupport._

import scala.concurrent.{ExecutionContext, Future}
import scala.languageFeature.implicitConversions
import scala.languageFeature.postfixOps
import scala.util.{Failure, Success}
import slick.driver.MySQLDriver.api.Database
import Device._
import org.genivi.sota.core.data.DeviceStatus.DeviceStatus

case class DeviceSearchResult(
                        namespace: Namespace,
                        id: Device.Id,
                        deviceName: DeviceName,
                        deviceId: Option[DeviceId],
                        deviceType: Device.DeviceType,
                        lastSeen: Option[Instant] = None,
                        status: Option[DeviceStatus] = None
                        )

class DevicesResource(db: Database, client: ConnectivityClient,
                      resolverClient: ExternalResolverClient,
                      deviceRegistry: DeviceRegistry,
                      namespaceExtractor: Directive1[Namespace])
                     (implicit system: ActorSystem, mat: ActorMaterializer) {

  import CirceMarshallingSupport._
  import Directives._
  import WebService._
  import system.dispatcher

  implicit val _db = db

  case object MissingDevice extends Throwable

  type RefinedRegx = Refined[String, Regex]

  /**
    * An ota client GET a Seq of [[Device]] from regex/status search.
    */
  def search(ns: Namespace): Route = {
    parameters(('status.?(false), 'regex.as[RefinedRegx].?)) {
      (includeStatus: Boolean, reqRegex: Option[RefinedRegx]) =>
        val regex = reqRegex.getOrElse(Refined.unsafeApply(".*")) // TODO optimize or forbid
        val devices = deviceRegistry.searchDevice(ns, regex)

        if (includeStatus) {
          val f = DeviceSearch.fetchDeviceStatus(devices)
          val response = f flatMap buildSearchResponse(devices)
          completeWith(response)
        } else {
          val response = buildSearchResponse(devices)(Seq.empty)
          completeWith(response)
        }
    }
  }

  protected def buildSearchResponse(devicesF: Future[Seq[Device]])(deviceStatus: Seq[DeviceUpdateStatus])
                                   (implicit ec: ExecutionContext): Future[Seq[DeviceSearchResult]] = {
    val statusById = deviceStatus.map(r => (r.device, r)).toMap

    devicesF map { _.map { d =>
      val deviceStatus = statusById.get(d.id)

      DeviceSearchResult(
        d.namespace,
        d.id,
        d.deviceName,
        d.deviceId,
        d.deviceType,
        deviceStatus.flatMap(_.lastSeen).orElse(d.lastSeen),
        deviceStatus.map(_.status)
      )
    }
    }
  }

  protected def completeWith[T](searchResult: Future[Seq[T]])(implicit ev: ToResponseMarshaller[Seq[T]]): Route = {
    onComplete(searchResult) {
      case Success(ds) => complete(ds)
      case Failure(ex) => extractLog { log =>
        log.error(ex, "cannot lookup update status for devices")
        complete((StatusCodes.InternalServerError, s"cannot lookup update status for devices: ${ex.getMessage}"))
      }
    }
  }

  val route =
    (pathPrefix("devices") & namespaceExtractor) { ns =>
      (pathEnd & get) { search(ns) }
    }
}
