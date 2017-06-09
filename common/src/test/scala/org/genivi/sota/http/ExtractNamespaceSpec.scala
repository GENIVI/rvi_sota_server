package org.genivi.sota.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.syntax.either._
import com.advancedtelematic.jws.{CompactSerialization, JwsPayload}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec}
import com.advancedtelematic.json.signature.JcaSupport._
import com.advancedtelematic.jwa.HS256

class ExtractNamespaceSpec extends PropSpec
  with PropertyChecks
  with ScalatestRouteTest
  with Matchers
  with Directives {

  import AuthNamespaceDirectives._
  import org.genivi.sota.Generators._

  def route: Route = (path("test") & authNamespace[IdToken](None)) { ns =>
    get { complete(StatusCodes.OK -> ns.get) }
  }

  property("namespace is deriveable from user context") {
    forAll(TokenGen, SecretKeyGen) { (token, key) =>
      val keyInfo = HS256.signingKey(key).toOption.get
      val jwsSerialized = CompactSerialization( HS256.withKey( JwsPayload(token), keyInfo) ).value
      Get("/test").withHeaders(Authorization(OAuth2BearerToken(jwsSerialized.toString))) ~>
        route ~> check { responseAs[String] shouldEqual token.subject.underlying }
    }
  }

  property("returns an unauthorized response if namespace is not available") {
    Get("/test") ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection.type]
    }
  }
}
