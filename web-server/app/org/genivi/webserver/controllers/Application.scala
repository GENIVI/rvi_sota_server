/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.webserver.controllers

import org.genivi.webserver.requesthelpers.{RightResponse, LeftResponse, ErrorResponse}
import org.slf4j.LoggerFactory
import play.api._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json.Json._

import javax.inject.Inject
import play.api.mvc._
import play.api.libs.ws._
import scala.concurrent.Future

import org.genivi.webserver.requesthelpers.RequestHelper._

class Application @Inject() (ws: WSClient) extends Controller {

  val coreHost = Play.current.configuration.getString("core.host").get
  val corePort = Play.current.configuration.getString("core.port").get
  val resolverHost = Play.current.configuration.getString("resolver.host").get
  val resolverPort = Play.current.configuration.getString("resolver.port").get
  val protocol = "http://"
  val auditLogger = LoggerFactory.getLogger("audit")
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def index: Action[AnyContent] = Action {
    Ok(views.html.main())
  }

  def reverseProxy(path: String) = Action.async(parse.raw) { request: Request[RawBuffer] =>
    val user = "unknown"
    // Mitigation for C04 : Log transactions to and from SOTA Server
    auditLogger.info(s"Request: $request from user $user")

    val RequestResponse = for {
      proxyResponseOne <- makeCoreRequest(request)
      proxyResponseTwo <- makeResolverRequest(request)
    } yield successfulResponse(proxyResponseOne, proxyResponseTwo)
    RequestResponse
  }

  def makeRequest(request: Request[RawBuffer], url: String): Future[WSResponse] = {
    WS.url(url + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withHeaders(parseHeaders(request.headers).toSeq: _*)
      .withQueryString(request.queryString.mapValues(_.head).toSeq: _*)
      .withBody(request.body.asBytes().get).execute
  }

  def makeCoreRequest(request: Request[RawBuffer]) = {
    makeRequest(request, protocol + coreHost + ":" + corePort)
  }

  def makeResolverRequest(request: Request[RawBuffer]) = {
    makeRequest(request, protocol + resolverHost + ":" + resolverPort)
  }

  def coreProxy(path: String) = Action.async(parse.raw) { request: Request[RawBuffer] =>
    val user = "unknown"
    // Mitigation for C04 : Log transactions to and from SOTA Server
    auditLogger.info(s"Request: $request from user $user")

    val RequestResponse = for {
      proxyResponseOne <- makeCoreRequest(request)
    } yield resultFromWsResponse(proxyResponseOne)
    RequestResponse
  }

  def parseHeaders(headers: Headers) = {
    val headersMap = headers.toMap.map { case( headerName, headerValue) =>
      headerName -> headerValue.mkString
    }
    headersMap
  }

  def successfulResponse(leftRes: WSResponse, rightRes: WSResponse): Result = {
    chooseResponse(leftRes.status, rightRes.status) match {
      case LeftResponse() => resultFromWsResponse(leftRes)
      case RightResponse() => resultFromWsResponse(rightRes)
      case ErrorResponse(msg) => BadRequest(toJson(Map("errorMsg" -> leftRes.body)))
    }
  }

  def installCampaign: Action[JsValue] = Action.async(parse.json) { request =>
    ws.url(protocol + coreHost + ":" + corePort + request.path).post(request.body).map { response =>
      resultFromWsResponse(response)
    }
  }


  def resultFromWsResponse(response : WSResponse) : Result = {
    val headers = response.allHeaders.mapValues(x => x.head)
    Result(header = ResponseHeader(status = response.status, headers = headers), body = Enumerator(response.bodyAsBytes))
  }
}
