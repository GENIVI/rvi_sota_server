package org.genivi.sota.core.jsonrpc

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import io.circe._
import org.genivi.sota.marshalling.{DeserializationException, CirceMarshallingSupport}
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalatest.{PropSpec, Matchers}
import CirceMarshallingSupport._
import org.scalatest.concurrent.ScalaFutures._

/**
 * Specs to test JSON-RPC requests running over HTTP Transport
 */
class HttpTransportSpec extends JsonRpcSpecBase {

  val transport = HttpTransport( Uri.Empty )

  val ResultStatusGen = Gen.oneOf( StatusCodes.OK, StatusCodes.Created, StatusCodes.Accepted )
  val marshaller = implicitly[ToEntityMarshaller[Json]]

  property("handles result of a request") {
    forAll(ResultStatusGen, Arbitrary.arbInt.arbitrary, JsonGen) { (statusCode: StatusCode, id: Int, result: Json) =>
      val resFuture = transport.handleRequestResult(
        HttpResponse(statusCode, entity = HttpEntity( ContentTypes.`application/json`, resultJson(id, result).spaces2 ) ))
      whenReady(resFuture){ res =>
        resultJson(id, result) shouldBe res
      }

    }
  }

  property("fails with error if status code indicates success but result is not a valid json") {
    forAll(ResultStatusGen, Gen.alphaStr) { (statusCode: StatusCode, entity) =>
      val resFuture = transport.handleRequestResult(
        HttpResponse(statusCode, entity = HttpEntity( ContentTypes.`application/json`, entity )))
      whenReady(resFuture.failed){ res =>
        res shouldBe a [ParsingFailure]
      }
    }
  }

  property( "if response contains json returns it despite error code") {
    forAll(Gen.oneOf( StatusCodes.NotFound, StatusCodes.InternalServerError ), Arbitrary.arbInt.arbitrary, JsonGen) { (statusCode: StatusCode, id: Int, result: Json) =>
      val resFuture = transport.handleRequestResult(HttpResponse(statusCode, entity = HttpEntity( ContentTypes.`application/json`, resultJson(id, result).spaces2 )))
      whenReady(resFuture){ res =>
        resultJson(id, result) shouldBe res
      }
    }
  }

  property( "if status code does not indicate success and entity is not a valid json fails with TransportException") {
    forAll(Gen.oneOf( StatusCodes.NotFound, StatusCodes.InternalServerError, StatusCodes.NoContent ), Gen.alphaStr) { (statusCode: StatusCode, str: String) =>
      val entity = if( statusCode.allowsEntity() ) HttpEntity( ContentTypes.`application/json`, str ) else HttpEntity.Empty
      val resFuture = transport.handleRequestResult(HttpResponse(statusCode, entity = entity))
      whenReady(resFuture.failed){ res =>
        res shouldBe a [client.TransportException]
      }
    }
  }

}
