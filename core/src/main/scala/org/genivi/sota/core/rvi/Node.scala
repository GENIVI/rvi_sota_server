/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core.rvi

import Protocol._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.ActorMaterializer
import org.genivi.sota.core.Package
import scala.concurrent.Future
import scala.util.control.NoStackTrace

trait RviInterface {

  def notify(vin: String, pkg: Package): Future[HttpResponse]
}

object RviInterface {
  case class UnexpectedRviResponse(response : HttpResponse) extends Throwable("Status code 200 OK expected") with NoStackTrace
}

class JsonRpcRviInterface(host: String, port: Int)
                         (implicit mat: ActorMaterializer, system: ActorSystem)
    extends RviInterface {

  implicit val ec = system.dispatcher

  private val uri: Uri =
    Uri().
      withScheme("http").
      withAuthority(host, port).
      withPath(Uri.Path("/"))

  def notify(vin: String, pkg: Package): Future[HttpResponse] = {
    val payload = JsonRpcRequest.notifyPackage(vin, pkg)
    val serialized = JsonRpcRequest.format[Notify].write(payload).toString()
    Http().singleRequest(HttpRequest(POST,
                                     uri = uri,
                                     entity = HttpEntity(`application/json`, serialized)))
      .flatMap {
      case r@HttpResponse(StatusCodes.OK, _, _, _) => Future.successful(r)
      case r@_ => Future.failed(RviInterface.UnexpectedRviResponse(r))
    }
  }
}
