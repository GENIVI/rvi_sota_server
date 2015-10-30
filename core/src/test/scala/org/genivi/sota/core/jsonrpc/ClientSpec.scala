package org.genivi.sota.core.jsonrpc

import cats.data.Xor
import io.circe._
import io.circe.syntax._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.Future
import scala.util.control.NoStackTrace

/**
 * Spec for testing JsonRpc requests from the SOTA Client
 */
class ClientSpec extends JsonRpcSpecBase {

  def respondWith( id: Int, result: Json ) : Json => Future[Json] = _ => Future.successful(
    resultJson(id, result)
  )

  property("handles successful response") {
    forAll { (id: Int, result: Json) =>
      val resultFuture = client.someMethod.request(1, id).run[Json]( respondWith( id, result) )
      whenReady( resultFuture ) { s =>
        s shouldBe result
      }

    }
  }

  property("handles failed requests") {
    forAll { (id: Int) =>
      val error = new Throwable("ups") with NoStackTrace
      val resultFuture = client.someMethod.request(1, id).run[String](_ => Future.failed( error ))
      whenReady( resultFuture.failed ) { s =>
        s shouldBe error
      }

    }
  }

  val UnmatchingIdsGen = for {
    first  <- Gen.choose(Int.MinValue, Int.MaxValue)
    second <- Gen.choose(Int.MinValue, Int.MaxValue).suchThat( _ != first)
  } yield (first, second)

  implicit val arbIds : Arbitrary[(Int, Int)] = Arbitrary( UnmatchingIdsGen )

  property("fails if response contains invalid id") {
    forAll { (ids: (Int, Int), result: Json) =>
      val resultFuture = client.someMethod.request(1, ids._1).run[Json](respondWith(ids._2, result))
      whenReady( resultFuture.failed ) { s =>
        s shouldBe (client.IdMismatchException.apply _).tupled(ids)
      }

    }
  }

  property("handles error responses") {
    forAll { (id: Int, errorCode: Int, message: String) =>
      val json = Json.obj(
        "jsonrpc" -> "2.0".asJson,
        "error" -> Json.obj(
          "code" -> errorCode.asJson,
          "message" -> message.asJson,
          "data" -> "none".asJson
        ),
        "id" -> id.asJson
      )

      whenReady(client.someMethod.request("a", id).run[String]( _ => Future.successful(json) ).failed) { e =>
        e shouldBe a [client.JsonRpcException]
        val errorResponse = e.asInstanceOf[client.JsonRpcException].response
        errorResponse.code shouldBe errorCode
        errorResponse.message shouldBe message
      }
    }
  }

}
