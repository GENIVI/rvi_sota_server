/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.webserver.controllers

import javax.inject.Inject

import jp.t2v.lab.play2.auth.{AuthElement, LoginLogout}
import org.genivi.webserver.Authentication.{Account, LdapAuth, User}
import org.slf4j.LoggerFactory
import play.api._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.ws._
import play.api.mvc._
import views.html

import scala.concurrent.{ExecutionContext, Future}

/**
 * The main application controller. Handles authentication and request proxying.
 *
 */
class Application @Inject() (ws: WSClient,
                             val messagesApi: MessagesApi,
                             ldapAuthN: LdapAuth,
                             configuration: play.api.Configuration)
  extends Controller with LoginLogout with AuthConfigImpl with I18nSupport with AuthElement {

  val auditLogger = LoggerFactory.getLogger("audit")
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val coreApiUri = configuration.getString("core.api.uri").get
  val resolverApiUri = configuration.getString("resolver.api.uri").get
  val deviceRegistryApiUri = configuration.getString("device_registry.api.uri").get

  /**
   * Returns an Option[String] of the uri of the service to proxy to
   *
   * @param path The path of the request
   * @return The service to proxy to
   */
  def apiByPath(path: String) : String = path.split("/").toList match {
    case "resolver" :: "packages" :: _ :: _ :: "filter" :: _ => resolverApiUri
    case "packages" :: _ => coreApiUri
    case "update_requests" :: _ => coreApiUri
    case "device_updates" :: _ => coreApiUri
    case "vehicle_updates" :: _ => coreApiUri
    case "history" :: _ => coreApiUri
    case "devices" :: _ => deviceRegistryApiUri
    case _ => resolverApiUri
  }

  /**
   * Proxies the request to the given service
   *
   * @param apiUri Uri of the service to proxy to
   * @param req request to proxy
   * @return The proxied request
   */
  def proxyTo(apiUri: String, ns: String, req: Request[RawBuffer]) : Future[Result] = {

    val allowedHeaders = Seq("content-type")
    def passHeaders(hdrs: Headers) = hdrs.toSimpleMap.filter(h => allowedHeaders.contains(h._1.toLowerCase)) +
      ("x-ats-namespace" -> ns)

    val w = ws.url(apiUri + req.path)
      .withFollowRedirects(false)
      .withMethod(req.method)
      .withHeaders(passHeaders(req.headers).toSeq :_*)
      .withQueryString(req.queryString.mapValues(_.head).toSeq :_*)

    val wreq = req.body.asBytes() match {
      case Some(b) => w.withBody(b)
      case None => w.withBody(FileBody(req.body.asFile))
    }

    wreq.stream.map { resp =>
      val rStatus = resp.headers.status
      val rHeaders = resp.headers.headers.mapValues(x => x.head)
      Result(
        header = ResponseHeader(rStatus, rHeaders),
        body = play.api.http.HttpEntity.Streamed(resp.body, contentLength = None, contentType = None)
      )
    }
  }

  /**
   * Proxies the given path
   *
   * @param path Path of the request
   * @return
   */
  def apiProxy(path: String) : Action[RawBuffer] = AsyncStack(parse.raw, AuthorityKey -> User) { implicit req =>
    { // Mitigation for C04: Log transactions to and from SOTA Server
      auditLogger.info(s"Request: $req from user ${loggedIn.id}")
    }
    proxyTo(apiByPath(path), loggedIn.id, req)
  }

  /**
   * Proxies request to both core and resolver
   *
   * @param path The path of the request
   * @return
   */
  def apiProxyBroadcast(path: String) : Action[RawBuffer] = AsyncStack(parse.raw, AuthorityKey -> User) {
    implicit req =>
    { // Mitigation for C04: Log transactions to and from SOTA Server
      auditLogger.info(s"Request: $req from user ${loggedIn.id}")
    }

    // Must PUT "vehicles" on both core and resolver
    // TODO: Retry until both responses are success
    for {
      respCore <- proxyTo(coreApiUri, loggedIn.id, req)
      respResult <- proxyTo(resolverApiUri, loggedIn.id, req)
    } yield respCore
  }

  def apiProxyWithNs[T](apiUri: String): Action[RawBuffer]
    = AsyncStack(parse.raw, AuthorityKey -> User) { implicit req =>
      auditLogger.info(s"Request: $req from user ${loggedIn.id}")

      val ns = loggedIn.id
      val reqNs = Request(req.copy(queryString = req.queryString + ("namespace" -> List(ns))), req.body)

      proxyTo(apiUri, ns, reqNs)
    }

  def listDeviceAttributes: Action[RawBuffer] = apiProxyWithNs(deviceRegistryApiUri)

  def resolver: Action[RawBuffer] = apiProxyWithNs(resolverApiUri)

  /**
   * Renders index.html
   *
   * @return OK response and index html
   */
  def index : Action[AnyContent] = StackAction(AuthorityKey -> User) { implicit req =>
    Ok(views.html.main())
  }

  val loginForm = Form (
    tuple("email" -> nonEmptyText, "password" -> nonEmptyText)
  )

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
    * A function that returns a `User` object from an `Id`.
    * You can alter the procedure to suit your application.
    */
  def resolveUser(id: Id)(implicit ctx: ExecutionContext): Future[Option[User]] =
    Future.successful( Some(Account(id, User)))

  /**
   * Authenticates a user
   *
   */
  def authenticate : Action[AnyContent]  = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors))),
      user => {
        ldapAuthN.authenticate(user._1, user._2)
          .flatMap{acc => gotoLoginSucceeded(user._1)}
          .recoverWith {
            case t =>
              Logger.debug("Login failed.", t)
              Future.successful(BadRequest(views.html.login(loginForm.fill(user._1 -> ""))))
            }
      }
    )
  }
}
