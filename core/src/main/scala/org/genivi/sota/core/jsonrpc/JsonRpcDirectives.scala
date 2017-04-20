/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.jsonrpc

import akka.http.scaladsl.server._
import io.circe.DecodingFailure
import io.circe._
import cats.syntax.either._
import org.genivi.sota.marshalling.CirceMarshallingSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * JSON-RPC request message.
 */
final case class Request( jsonrpc: String, method: String, params: Json, id: Option[Int] )

object PredefinedErrors {

  val ParseError = JsonRpcError(-32700, "Parse error") // scalastyle:ignore magic.number

  object InvalidRequest {
    def apply(data: Json): JsonRpcError =
      JsonRpcError(-32600, "Invalid request", Some(data)) // scalastyle:ignore magic.number
  }

  val MethodNotFound = JsonRpcError(-32601, "Method not found") // scalastyle:ignore magic.number

  val InvalidParams = JsonRpcError(-32602, "Invalid params") // scalastyle:ignore magic.number
}

private[this] case class ResultResponse(jsonrpc: String, result: Json, id: Option[Int])

private[this] object ResultResponse {

  def apply[T](t: T, id: Option[Int])(implicit et: Encoder[T]) : ResultResponse =
    ResultResponse("2.0", et(t), id)

  import io.circe.generic.semiauto._
  implicit val encoderInstance = deriveEncoder[ResultResponse]
}

/**
 * Directives for handling JSON-RPC messages.
 */
trait JsonRpcDirectives {

  import akka.http.scaladsl.server.Directives._
  import CirceMarshallingSupport._
  import io.circe.generic.auto._
  import io.circe.syntax._

  type MethodFn = Request => StandardRoute

  import scala.language.implicitConversions

  implicit def lift[In, Out](fn: In => Future[Out])
                   (implicit inDecoder: Decoder[In], outEncoder: Encoder[Out], ec: ExecutionContext)
      : MethodFn = request => {

    inDecoder.decodeJson( request.params ).map( fn ).fold[StandardRoute](
      err =>
        complete(ErrorResponse( PredefinedErrors.InvalidParams, request.id)),
      res => complete( res.map( x => ResultResponse(x, request.id)  )))
  }

  val ParseErrorHandler = RejectionHandler.newBuilder().handle{
    case MalformedRequestContentRejection(_, ParsingFailure(_, _)) =>
      complete(ErrorResponse( PredefinedErrors.ParseError, None ) )

    case MalformedRequestContentRejection(_, DecodingFailure(msg, _)) =>
      complete(ErrorResponse( PredefinedErrors.InvalidRequest(msg.asJson), None ) )

    case ml @ MalformedRequestContentRejection(msg, _) =>
      complete(ErrorResponse( PredefinedErrors.InvalidRequest(msg.asJson), None ) )

  }.result()

  def service(methods: (String, MethodFn)*) : Route = service(methods.toMap)

  def service(methods: Map[String, MethodFn]) : Route = extractMaterializer { implicit mat =>
    (handleRejections(ParseErrorHandler) & entity(as[Request])) { request =>
      methods.get(request.method).fold(complete(ErrorResponse(PredefinedErrors.MethodNotFound, request.id)))(_(request))
    }
  }

}

object JsonRpcDirectives extends JsonRpcDirectives
