package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import DefaultJsonProtocol._
import akka.http.scaladsl.unmarshalling.Unmarshal

class Resolver(host: String, port: Int)(implicit system : ActorSystem,
                                        mat: ActorMaterializer,
                                        exec: ExecutionContext) {
  val uri: Uri =
    Uri().
      withScheme("http").
      withAuthority(host, port)


  def resolve(packageId: Long): Future[Map[Vin, Set[Long]]] =
    request(packageId)
      .flatMap { response =>
      Unmarshal(response.entity).to[Map[String, Set[Long]]].map { parsed =>
        parsed.map { case (k, v) => Vin(k) -> v }
      }
    }.recover { case _ => Map.empty[Vin, Set[Long]] }

  private def request(packageId: Long): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(uri = uri.withPath(Uri.Path(s"/api/v1/resolve/$packageId"))))
  }
}
