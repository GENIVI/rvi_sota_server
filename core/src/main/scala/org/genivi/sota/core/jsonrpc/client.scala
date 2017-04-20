/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.jsonrpc

import io.circe.Decoder

import scala.language.dynamics
import scala.util.control.NoStackTrace
import cats.syntax.either._

/**
 * Encoder/decoder for JSON-RPC requests and responses.
 */
// scalastyle:off
object client extends Dynamic {
// scalastyle:on
  import scala.concurrent.{ExecutionContext, Future}

  type Method = String

  final case class Response[A](result: A, id: Int)

  object Response {

    implicit def decoderInstance[A](implicit da: Decoder[A]): Decoder[Response[A]] = Decoder.instance( c =>
      for{
        r  <- c.downField("result").as[A]
        id <- c.downField("id").as[Int]
      } yield Response(r, id)
    )

  }

  case class TransportException(msg: String) extends Throwable(msg) with NoStackTrace

  case class JsonRpcException(response: JsonRpcError)
      extends Throwable("Error response received from server.") with NoStackTrace

  case class IdMismatchException(expected: Int, received: Int)
      extends Throwable(s"Id in response ($received) does not match id in request ($expected).") with NoStackTrace

  import io.circe._
  import io.circe.syntax._

  private[jsonrpc] def responseDecoder[A](implicit da: Decoder[A]): Decoder[ErrorResponse Either Response[A]] =
    Decoder.instance { c =>
      val ed = Decoder[ErrorResponse]
      val rd = Decoder[Response[A]]
      for {
        version <- c.get[String]("jsonrpc")
        _       <- if (version == "2.0") { Right(version) }
                   else { Left(DecodingFailure(s"Illegal version: $version", c.history)) }
        res     <- (c.downField("error").success, c.downField("result").success) match {
          case (Some(_), None) => ed(c).map(Left.apply)
          case (None, Some(_)) => rd(c).map(Right.apply)
          case _ => Left(DecodingFailure("Either the result member or error member MUST be included.", c.history))
        }
      } yield res

    }

  private[jsonrpc] def extractResponse[A](json: Json)(implicit da : Decoder[A]): Future[Response[A]] = {
    responseDecoder[A]
      .decodeJson(json)
      .fold(Future.failed, _.fold( err => Future.failed(JsonRpcException(err.error)), Future.successful) )
  }


  def selectDynamic( name: String ) : JsonRpcMethod = new JsonRpcMethod(name)

  final class JsonRpcMethod(name: String) {
    def request[A](a: A, id: Int)(implicit encoder: Encoder[A]) : Request = Request( name, encoder(a), id )

    def notification[A](a: A)(implicit encoder: Encoder[A]) : Notification = Notification( name, encoder(a) )
  }

  final case class Request(method: String, params: Json, id: Int) {
    def run[A](transport: Json => Future[Json])(implicit decoder: Decoder[A], exec: ExecutionContext): Future[A] = {
      val request =
        Json.obj(
          "jsonrpc" -> "2.0".asJson,
          "method" -> method.asJson,
          "params" -> params,
          "id" -> id.asJson)
      for {
        json     <- transport(request)
        response <- extractResponse[A](json)
        _        <- if( response.id != id ) { Future.failed( IdMismatchException(id, response.id) ) }
                    else { Future.successful(response) }
      } yield response.result
    }
  }

  final case class Notification(method: String, params: Json) {

    def run( transport: Json => Future[Unit]): Future[Unit] = ???

  }

}
