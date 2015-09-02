/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.webserver.controllers

import javax.inject.Inject

import jp.t2v.lab.play2.auth.{AuthElement, LoginLogout}
import org.genivi.webserver.Authentication.{AccountManager, Role}
import org.genivi.webserver.requesthelpers.RequestHelper._
import org.genivi.webserver.requesthelpers.{ErrorResponse, LeftResponse, RightResponse}
import org.slf4j.LoggerFactory
import play.api.Play.current
import play.api._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.libs.ws._
import play.api.mvc._
import views.html

import scala.concurrent.{ExecutionContext, Future}

class Application @Inject() (ws: WSClient, val messagesApi: MessagesApi, val accountManager: AccountManager)
  extends Controller with LoginLogout with AuthConfigImpl with I18nSupport with AuthElement {

  val coreHost = Play.current.configuration.getString("core.host").get
  val corePort = Play.current.configuration.getString("core.port").get
  val resolverHost = Play.current.configuration.getString("resolver.host").get
  val resolverPort = Play.current.configuration.getString("resolver.port").get
  val protocol = "http://"
  val auditLogger = LoggerFactory.getLogger("audit")
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def index = StackAction(AuthorityKey -> Role.USER) { implicit request =>
    Ok(views.html.main())
  }

  def reverseProxyNoBody(path: String) = AsyncStack(AuthorityKey -> Role.USER) { implicit request =>
    val user = loggedIn.name
    // Mitigation for C04 : Log transactions to and from SOTA Server
    auditLogger.info(s"Request: $request from user $user")

    val RequestResponse = for {
      proxyResponseOne <- makeRequestNoBody(RequestTarget.Core, request)
      proxyResponseTwo <- makeRequestNoBody(RequestTarget.Resolver, request)
    } yield successfulResponse(proxyResponseOne, proxyResponseTwo)
    RequestResponse
  }

  def reverseProxy(path: String) = AsyncStack(parse.json, AuthorityKey -> Role.USER) { implicit request =>
    val user = loggedIn.name
    // Mitigation for C04 : Log transactions to and from SOTA Server
    auditLogger.info(s"Request: $request from user $user")

    val RequestResponse = for {
      proxyResponseOne <- makeRequestJson(RequestTarget.Core, request)
      proxyResponseTwo <- makeRequestJson(RequestTarget.Resolver, request)
    } yield successfulResponse(proxyResponseOne, proxyResponseTwo)
    RequestResponse
  }

  def resolverProxy = AsyncStack(parse.raw, AuthorityKey -> Role.USER) { implicit request =>
    val user = loggedIn.name
    // Mitigation for C04 : Log transactions to and from SOTA Server
    auditLogger.info(s"Request: $request from user $user")

    val RequestResponse = for {
      response <- makeRequestRaw(RequestTarget.Resolver, request)
    } yield resultFromWsResponse(response)
    RequestResponse
  }

  def coreProxy(path: String) = AsyncStack(parse.raw, AuthorityKey -> Role.USER) { implicit request =>
    val user = loggedIn.name
    // Mitigation for C04 : Log transactions to and from SOTA Server
    auditLogger.info(s"Request: $request from user $user")

    val RequestResponse = for {
      response <- makeRequestRaw(RequestTarget.Core, request)
    } yield resultFromWsResponse(response)
    RequestResponse
  }

  def makeRequestNoBody(requestTarget: RequestTarget.RequestTarget,
                      request: Request[_]): Future[WSResponse] = {
    WS.url(getURL(requestTarget) + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withHeaders(parseHeaders(request.headers).toSeq: _*)
      .withQueryString(request.queryString.mapValues(_.head).toSeq: _*)
      .execute
  }

  def makeRequestJson(requestTarget: RequestTarget.RequestTarget,
                      request: Request[JsValue]): Future[WSResponse] = {
    WS.url(getURL(requestTarget) + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withHeaders(parseHeaders(request.headers).toSeq: _*)
      .withQueryString(request.queryString.mapValues(_.head).toSeq: _*)
      .withBody(request.body).execute
  }

  def makeRequestRaw(requestTarget: RequestTarget.RequestTarget,
                     request: Request[RawBuffer]): Future[WSResponse] = {
    WS.url(getURL(requestTarget) + request.path)
      .withFollowRedirects(false)
      .withMethod(request.method)
      .withHeaders(parseHeaders(request.headers).toSeq: _*)
      .withQueryString(request.queryString.mapValues(_.head).toSeq: _*)
      .withBody(request.body.asBytes().get).execute
  }

  def getURL(requestTarget: RequestTarget.RequestTarget) = {
    requestTarget match {
      case RequestTarget.Core => protocol + coreHost + ":" + corePort
      case RequestTarget.Resolver => protocol + resolverHost + ":" + resolverPort
    }
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

  def resultFromWsResponse(response : WSResponse) : Result = {
    val headers = response.allHeaders.mapValues(x => x.head)
    Result(header = ResponseHeader(status = response.status, headers = headers), body = Enumerator(response.bodyAsBytes))
  }

  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] = {
    Future.successful(accountManager.findById(id))
  }

  val loginForm = Form {
    mapping("email" -> email, "password" -> nonEmptyText)(accountManager.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { request =>
    Ok(html.login(loginForm))
  }

  def logout = Action.async{ implicit request =>
    gotoLogoutSucceeded
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors))),
      user => gotoLoginSucceeded(user.get.email)
    )
  }

  object RequestTarget extends Enumeration {
    type RequestTarget = Value
    val Core, Resolver = Value
  }

}
