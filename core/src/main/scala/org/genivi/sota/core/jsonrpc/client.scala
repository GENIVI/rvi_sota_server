/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.jsonrpc

import io.circe.Decoder

import scala.language.dynamics
import scala.util.control.NoStackTrace

/**
 * Encoder/decoder for JSON-RPC requests and responses.
 */
// scalastyle:off
object client extends Dynamic {
// scalastyle:on
  import cats.data.Xor
  import scala.concurrent.ExecutionContext
  import scala.concurrent.Future

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

  private[jsonrpc] def responseDecoder[A](implicit da: Decoder[A]): Decoder[ErrorResponse Xor Response[A]] =
    Decoder.instance { c =>
      val ed = Decoder[ErrorResponse]
      val rd = Decoder[Response[A]]
      for {
        version <- c.get[String]("jsonrpc")
        _       <- if (version == "2.0") Xor.right(version)
                   else Xor.Left(DecodingFailure(s"Illegal version: $version", c.history))
        res     <- (c.downField("error").success, c.downField("result").success) match {
          case (Some(_), None) => ed(c).map(Xor.left)
          case (None, Some(_)) => rd(c).map(Xor.right)
          case _ => Xor.left(DecodingFailure("Either the result member or error member MUST be included.", c.history))
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

    import shapeless._
    import record._
    import syntax.singleton._
    import io.circe.generic.auto._

    def run[A](transport: Json => Future[Json])(implicit decoder: Decoder[A], exec: ExecutionContext): Future[A] = {
      val request = ('jsonrpc ->> "2.0") :: ('method ->> method) :: ('params ->> params) :: ('id ->> id) :: HNil
      for {
        json     <- transport(request.asJson)
        response <- extractResponse[A](json)
        _        <- if( response.id != id ) Future.failed( IdMismatchException(id, response.id) )
                    else Future.successful(response)
      } yield response.result
    }

  }

  final case class Notification(method: String, params: Json) {

    def run( transport: Json => Future[Unit]): Future[Unit] = ???

  }

}
