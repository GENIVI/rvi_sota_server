/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core.rvi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.util.ByteString
import org.genivi.sota.core.data.{Package, Vehicle}
import spray.json.JsonFormat

import scala.concurrent.Future
import scala.util.control.NoStackTrace

trait RviInterface {
  def notify(vin: Vehicle.IdentificationNumber, pkg: Package): Future[HttpResponse]
  def transferStart(transactionId: Long, destination: String, packageIdentifier: String, totalSize: Long, chunkSize: Long, checksum: String): Future[HttpResponse]
  def transferChunk(transactionId: Long, destination: String, index: Long, msg: ByteString): Future[HttpResponse]
  def transferFinish(transactionId: Long, destination: String): Future[HttpResponse]
}

object RviInterface {
  case class UnexpectedRviResponse(response : HttpResponse) extends Throwable("Status code 200 OK expected") with NoStackTrace
}

class JsonRpcRviInterface(host: String, port: Int)
                         (implicit mat: Materializer, system: ActorSystem) extends RviInterface {

  implicit val ec = system.dispatcher

  private val uri: Uri =
    Uri().
      withScheme("http").
      withAuthority(host, port).
      withPath(Uri.Path("/"))

  import Protocol._

  private def performRequest[A <: Action :JsonFormat](req: JsonRpcRequest[A]): Future[HttpResponse] = {
    val serialized = JsonRpcRequest.format[A].write(req).toString()
    println(s"Sending $serialized to RVI Node at $uri")
    Http().singleRequest(HttpRequest(POST, uri = uri, entity = HttpEntity(`application/json`, serialized)))
      .flatMap {
      case r@HttpResponse(StatusCodes.OK, _, _, _) => { println(s"sucess $req: $r"); Future.successful(r) }
      case r@_ =>  { println(s"failed $req! $r"); Future.failed(RviInterface.UnexpectedRviResponse(r)) }
    }
  }

  def notify(vin: Vehicle.IdentificationNumber, pkg: Package): Future[HttpResponse] = performRequest(JsonRpcRequest.notifyPackage(vin, pkg))

  def transferStart(transactionId: Long, destination: String, packageIdentifier: String, totalSize: Long, chunkSize: Long, checksum: String): Future[HttpResponse] =
    performRequest(JsonRpcRequest.transferStart(transactionId, destination, packageIdentifier, totalSize, chunkSize, checksum))

  def transferChunk(transactionId: Long, destination: String, index: Long, msg: ByteString): Future[HttpResponse] =
    performRequest(JsonRpcRequest.transferChunk(transactionId, destination, index, msg))

  def transferFinish(transactionId: Long, destination: String): Future[HttpResponse] =
    performRequest(JsonRpcRequest.transferFinish(transactionId, destination))
}
