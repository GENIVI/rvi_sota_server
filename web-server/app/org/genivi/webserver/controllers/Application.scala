/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.webserver.controllers

import javax.inject.Inject

import jp.t2v.lab.play2.auth.{AuthElement, LoginLogout}
import org.genivi.webserver.Authentication.{AccountManager, Role}
import org.slf4j.LoggerFactory
import play.api.Play.current
import play.api._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.iteratee.Enumerator
import play.api.libs.ws._
import play.api.mvc._
import views.html

import scala.concurrent.{ExecutionContext, Future}

/**
 * The main application controller. Handles authentication and request proxying.
 *
 */
class Application @Inject() (ws: WSClient, val messagesApi: MessagesApi, val accountManager: AccountManager)
  extends Controller with LoginLogout with AuthConfigImpl with I18nSupport with AuthElement {

  val auditLogger = LoggerFactory.getLogger("audit")
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val coreApiUri = Play.current.configuration.getString("core.api.uri").get
  val resolverApiUri = Play.current.configuration.getString("resolver.api.uri").get

  /**
   * Returns an Option[String] of the uri of the service to proxy to
   *
   * @param path The path of the request
   * @return The service to proxy to
   */
  def apiByPath(path: String) : String = path.split("/").toList match {
    case "packages" :: _ :: _ :: "filter" :: _ => resolverApiUri
    case "packages" :: _ => coreApiUri
    case "updates" :: _ => coreApiUri
    case "vehicles" :: vin :: part :: _
      if (Set("queued", "history", "sync")(part)) => coreApiUri
    case _ => resolverApiUri
  }

  /**
   * Proxies the request to the given service
   *
   * @param apiUri Uri of the service to proxy to
   * @param req request to proxy
   * @return The proxied request
   */
  def proxyTo(apiUri: String, req: Request[RawBuffer]) : Future[Result] = {
    def toWsHeaders(hdrs: Headers) = hdrs.toMap.map {
      case(name, value) => name -> value.mkString }

    val w = WS.url(apiUri + req.path)
      .withFollowRedirects(false)
      .withMethod(req.method)
      .withHeaders(toWsHeaders(req.headers).toSeq :_*)
      .withQueryString(req.queryString.mapValues(_.head).toSeq :_*)

    val wreq = req.body.asBytes() match {
      case Some(b) => w.withBody(b)
      case None => w.withBody(FileBody(req.body.asFile))
    }
    wreq.execute.map { resp =>
      Result(
        header = ResponseHeader(resp.status, resp.allHeaders.mapValues(x => x.head)),
        body = Enumerator(resp.bodyAsBytes))
    }
  }

  /**
   * Proxies the given path
   *
   * @param path Path of the request
   * @return
   */
  def apiProxy(path: String) : Action[RawBuffer] = AsyncStack(parse.raw, AuthorityKey -> Role.USER) { implicit req =>
    { // Mitigation for C04: Log transactions to and from SOTA Server
      auditLogger.info(s"Request: $req from user ${loggedIn.name}")
    }
    proxyTo(apiByPath(path), req)
  }

  /**
   * Proxies request to both core and resolver
   *
   * @param path The path of the request
   * @return
   */
  def apiProxyBroadcast(path: String) : Action[RawBuffer] = AsyncStack(parse.raw, AuthorityKey -> Role.USER) {
    implicit req =>
    { // Mitigation for C04: Log transactions to and from SOTA Server
      auditLogger.info(s"Request: $req from user ${loggedIn.name}")
    }

    // Must PUT "vehicles" on both core and resolver
    // TODO: Retry until both responses are success
    for {
      respCore <- proxyTo(coreApiUri, req)
      respResult <- proxyTo(resolverApiUri, req)
    } yield respCore
  }

  /**
   * Renders index.html
   *
   * @return OK response and index html
   */
  def index : Action[AnyContent] = StackAction(AuthorityKey -> Role.USER) { implicit req =>
    Ok(views.html.main())
  }

  /**
   * Find a user by id
   *
   * @param id The Id of the user
   * @return Future option of user
   */
  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] = {
    Future.successful(accountManager.findById(id))
  }

  val loginForm = Form {
    mapping("email" -> email, "password" -> nonEmptyText)(accountManager.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  /**
   * Renders the login form
   *
   * @return OK response and login.html
   */
  def login : Action[AnyContent] = Action { request =>
    Ok(html.login(loginForm))
  }

  /**
   * Logs out a user
   *
   */
  def logout : Action[AnyContent] = Action.async{ implicit request =>
    gotoLogoutSucceeded
  }

  /**
   * Authenticates a user
   *
   */
  def authenticate : Action[AnyContent]  = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors))),
      user => gotoLoginSucceeded(user.get.email)
    )
  }
}
