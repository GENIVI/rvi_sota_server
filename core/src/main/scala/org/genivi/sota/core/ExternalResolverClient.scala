/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import org.genivi.sota.core.data.Vehicle
import org.genivi.sota.core.data.PackageId
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import DefaultJsonProtocol._
import akka.http.scaladsl.unmarshalling.Unmarshal

trait ExternalResolverClient {

  def putPackage(packageId: PackageId, description: Option[String], vendor: Option[String]): Future[Unit]

  def resolve(packageId: PackageId): Future[Map[Vehicle, Set[PackageId]]]
}

class DefaultExternalResolverClient(baseUri : Uri)
                                   (implicit system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext) extends ExternalResolverClient {

  import org.genivi.sota.refined.SprayJsonRefined._

  override def resolve(packageId: PackageId): Future[Map[Vehicle, Set[PackageId]]] =
    request(packageId).flatMap { response =>
      Unmarshal(response.entity).to[Map[Vehicle.IdentificationNumber, Set[PackageId]]].map { parsed =>
        parsed.map { case (k, v) => Vehicle(k) -> v }
      }
    }.recover { case _ => Map.empty[Vehicle, Set[PackageId]] }

  override def putPackage(packageId: PackageId, description: Option[String], vendor: Option[String]): Future[Unit] = ???

  private def request(packageId: PackageId): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = baseUri.withPath( baseUri.path / packageId.name.get / packageId.version.get)))
  }
}
