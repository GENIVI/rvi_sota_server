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
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import DefaultJsonProtocol._
import akka.http.scaladsl.unmarshalling.Unmarshal

class Resolver(host: String, port: Int)(implicit system : ActorSystem,
                                        mat: ActorMaterializer,
                                        exec: ExecutionContext) {
  val uri: Uri = Uri().withScheme("http").withAuthority(host, port)

  import org.genivi.sota.refined.SprayJsonRefined._
  def resolve(packageId: Long): Future[Map[Vehicle, Set[Long]]] =
    request(packageId).flatMap { response =>
      Unmarshal(response.entity).to[Map[Vehicle.IdentificationNumber, Set[Long]]].map { parsed =>
        parsed.map { case (k, v) => Vehicle(k) -> v }
      }
    }.recover { case _ => Map.empty[Vehicle, Set[Long]] }

  private def request(packageId: Long): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = uri.withPath(Uri.Path(s"/api/v1/resolve/$packageId"))))
  }
}
