package org.genivi.sota.http

import java.util.UUID

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directives, Route}
import org.scalatest.{FunSuite, ShouldMatchers}
import akka.http.scaladsl.testkit.ScalatestRouteTest

class TraceIdSpec extends FunSuite
  with ScalatestRouteTest
  with ShouldMatchers {

  import Directives._

  def route: Route = (path("test") & TraceId.withTraceId) {
    get { complete(StatusCodes.NoContent) }
  }

  def traceIdHeader(response: HttpResponse): Option[String] = {
    response.headers
      .find(_.is(TraceId.TRACEID_HEADER))
      .map(_.value)
      .map(UUID.fromString)
      .map(_.toString)
  }

  def traceIdSigHeader(response: HttpResponse): Option[String] = {
    response.headers
      .find(_.is(TraceId.TRACEID_HMAC_HEADER))
      .map(_.value)
  }

  test("adds traceid if there isn't one in headers") {
    Get("/test") ~> route ~> check {
      traceIdHeader(response) shouldBe defined
      traceIdSigHeader(response) shouldBe defined
    }
  }

  test("adds traceid if header is invalid") {
    Get("/test").withHeaders(RawHeader(TraceId.TRACEID_HEADER, "invalid")) ~> route ~> check {
      traceIdHeader(response) shouldBe defined
      traceIdSigHeader(response) shouldBe defined
    }
  }

  test("adds traceid if header sig is invalid") {
    Get("/test")
      .withHeaders(
        RawHeader(TraceId.TRACEID_HEADER, UUID.randomUUID().toString),
        RawHeader(TraceId.TRACEID_HMAC_HEADER, "invalid sig")
    ) ~> route ~> check {
      traceIdHeader(response) shouldBe defined
      traceIdSigHeader(response) shouldBe defined
    }
  }

  test("keeps traceid header if header is valid") {
    val traceId = UUID.randomUUID().toString
    val sig = TraceIdSig(traceId).get

    Get("/test").withHeaders(RawHeader(TraceId.TRACEID_HEADER, traceId),
      RawHeader(TraceId.TRACEID_HMAC_HEADER, sig)) ~> route ~> check {

      traceIdHeader(response) should contain(traceId)
      traceIdSigHeader(response) should contain(sig)
    }
  }

  test("adds valid sig") {
    Get("/test") ~> route ~> check {
      val id = traceIdHeader(response).get
      val sig = traceIdSigHeader(response).get

      sig shouldBe TraceIdSig(id).get
    }
  }
}
