package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class Resolver(host: String, port: Int)(implicit system : ActorSystem,
                                        mat: ActorMaterializer,
                                        exec: ExecutionContext) {

  // TODO: Remove when we can actually connect to the resolver
  val stub = Map(Vin("VIN001") -> Set(1.toLong, 2.toLong))

  def resolve(packageId: Long): Future[Map[Vin, Set[Long]]] =
    request(packageId)
      .map[Map[Vin, Set[Long]]] { response => stub }
      .recover { case _ => stub }

  private def request(packageId: Long): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(uri = s"http://$host:$port/resolve/$packageId"))
}
