package org.genivi.sota.http

import java.util.UUID

import collection.JavaConversions._
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directive0

import scala.util.{Failure, Success, Try}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import com.typesafe.config.ConfigFactory

object TraceIdSig {
  def apply(msg: String): Try[String] = Try {
    mac.reset()
    val bytes = mac.doFinal(msg.getBytes("ASCII"))
    val hash = new StringBuffer()

    bytes.foreach { b =>
      val hex = Integer.toHexString(0xFF & b)
      if (hex.length() == 1) {
        hash.append('0')
      }
      hash.append(hex)
    }

    hash.toString
  }

  private lazy val key = {
    val c = Try(ConfigFactory.load().getString("traceid.key")).getOrElse("Thohdoobu9")
    new SecretKeySpec(c.getBytes("UTF-8"), "HmacSHA1")
  }

  private lazy val mac = {
    val m = Mac.getInstance("HmacSHA1")
    m.init(key)
    m
  }
}

object TraceId {

  import akka.http.scaladsl.server.Directives._

  val TRACEID_HEADER = "x-ats-traceid"
  val TRACEID_HMAC_HEADER = "x-ats-trace-sig"

  case class TraceId(id: String, sig: String)

  def parseValidHeader(header: Option[String], sig: Option[String]): Option[TraceId] = {
    for {
      headerV <- header
      sigV <- sig
      uuid <- Try(UUID.fromString(headerV)).toOption
      uuidSig <- TraceIdSig(uuid.toString).toOption
      if uuidSig == sigV
    } yield TraceId(headerV, sigV)
  }

  val traceMetrics = (req: HttpRequest, resp: HttpResponse) => {
    Map("traceid" -> req.headers.find(_.is(TRACEID_HEADER)).map(_.value()).getOrElse("?"))
  }

  private def traceIdHeaders(traceId: TraceId): List[HttpHeader] = {
    List(RawHeader(TRACEID_HEADER, traceId.id), RawHeader(TRACEID_HMAC_HEADER, traceId.sig))
  }

  def withTraceId: Directive0 = {
    mapRequest{ r =>
      val traceIdHeader = r.headers.find(_.is(TRACEID_HEADER)).map(_.value())
      val traceIdSig = r.headers.find(_.is(TRACEID_HMAC_HEADER)).map(_.value())

      val existingHeader = parseValidHeader(traceIdHeader, traceIdSig)

      existingHeader match {
        case Some(_) => r
        case None =>
          r
            .removeHeader(TRACEID_HEADER)
            .removeHeader(TRACEID_HMAC_HEADER)
            .addHeaders(traceIdHeaders(newTraceId))
      }
    } tflatMap { _ =>
      extractRequest flatMap { r =>
        val idH = r.headers.find(_.is(TRACEID_HEADER)).map(_.value).getOrElse("<traceid error>")
        val sigH = r.headers.find(_.is(TRACEID_HMAC_HEADER)).map(_.value).getOrElse("<traceid sig error>")

        respondWithHeaders(traceIdHeaders(TraceId(idH, sigH)))
      }
    }
  }

  def newTraceId: TraceId = {
    val newId = UUID.randomUUID().toString

    TraceIdSig(newId) match {
      case Success(s) => TraceId(newId, s)
      case Failure(t) =>
        println(t.getMessage)
        TraceId(newId, "<error>")
    }
  }
}
