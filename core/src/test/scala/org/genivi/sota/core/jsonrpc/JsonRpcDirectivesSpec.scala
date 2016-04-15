package org.genivi.sota.core.jsonrpc

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import io.circe.Json
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import CirceMarshallingSupport._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.concurrent.ScalaFutures._
import scala.concurrent.Future

/**
 * Property-based spec for testing JSON-RPC directives
 */
class JsonRpcDirectivesSpec extends PropSpec
    with PropertyChecks
    with Matchers
    with akka.http.scaladsl.testkit.ScalatestRouteTest
    with JsonRpcDirectives {

  property("parse errors") {
    Post("/").withEntity( HttpEntity(ContentTypes.`application/json`, "dafdsfasfasf") ) ~>
        service( Map.empty[String, MethodFn]) ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[ErrorResponse].error.data.get.asString.get should include("expected json value")
          responseAs[ErrorResponse].error.message shouldBe "Invalid request"
        }
  }

  import org.genivi.sota.core.jsonrpc.JsonGen._
  import shapeless._
  import syntax.singleton._

  property("invalid requests") {
    forAll{ (json: Json) =>
      Post("/",  ('method ->> "bla") :: ('params ->> json) :: HNil ) ~> service( Map.empty[String, MethodFn] ) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[ErrorResponse].error should matchPattern { case JsonRpcError(-32600, "Invalid request", _) => }
      }
    }
  }

  val fn1: Int => Future[String] = x => Future.successful(s"fn1: $x")
  val fn2: String => Future[Int] = x => Future.successful(x.length())

  property("call method") {
    val underTest = service( "one" -> fn1, "two" -> fn2 )
    forAll { (params: String) =>
      Post("/",  ('jsonrpc ->> "2.0") :: ('method ->> "two") :: ('params ->> params) :: ('id ->> 1) :: HNil ) ~> underTest ~> check {
        status shouldBe StatusCodes.OK
        fn2(params).futureValue shouldBe responseAs[client.Response[Int]].result
      }
    }
    forAll { (params: Int) =>
      Post("/",  ('jsonrpc ->> "2.0") :: ('method ->> "one") :: ('params ->> params) :: ('id ->> 1) :: HNil ) ~> underTest ~> check {
        status shouldBe StatusCodes.OK
        responseAs[client.Response[String]].result shouldBe fn1(params).futureValue
      }
    }
  }

  property("Invalid params") {
    val underTest = service( "one" -> fn1, "two" -> fn2 )
    forAll( Gen.oneOf(JBooleanGen, JNumGen, JNullGen )) { (params: Json) =>
      Post("/",  ('jsonrpc ->> "2.0") :: ('method ->> "two") :: ('params ->> params) :: ('id ->> 1) :: HNil ) ~> underTest ~> check {
        status shouldBe StatusCodes.OK
        responseAs[ErrorResponse].error shouldBe PredefinedErrors.InvalidParams
      }
    }
    forAll( Gen.oneOf(JBooleanGen, JStrGen, JNullGen) ) { (params: Json) =>
      Post("/",  ('jsonrpc ->> "2.0") :: ('method ->> "one") :: ('params ->> params) :: ('id ->> 1) :: HNil ) ~> underTest ~> check {
        status shouldBe StatusCodes.OK
        responseAs[ErrorResponse].error shouldBe PredefinedErrors.InvalidParams
      }
    }
  }

  property("method not found") {
    forAll { (params: Json) =>
      Post("/",  ('jsonrpc ->> "2.0") :: ('method ->> "bla") :: ('params ->> params) :: ('id ->> 1) :: HNil ) ~> service( Map( "aaa" -> lift( (s: String) => Future.successful("  ") ))) ~> check {
        status shouldBe StatusCodes.OK
        responseAs[ErrorResponse].error shouldBe PredefinedErrors.MethodNotFound
      }
    }
  }
}
