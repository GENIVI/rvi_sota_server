/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.webserver.controllers

import org.genivi.webserver.requesthelpers.{RightResponse, LeftResponse, ErrorResponse}
import play.api._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsValue
import play.api.mvc._

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
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def index: Action[AnyContent] = Action {
    Ok(views.html.main())
  }

  def apiProxy(path: String): Action[JsValue] = Action.async(parse.json) { request =>
    val RequestResponse: Future[Result] = for {
      responseOne <- ws.url(protocol + coreHost + ":" + corePort + request.path).post(request.body)
      responseTwo <- ws.url(protocol + resolverHost + ":" + resolverPort + request.path)
        .post(request.body)
    } yield {
        chooseResponse(responseOne.status, responseTwo.status) match {
          case LeftResponse() => resultFromWsResponse(responseOne)
          case RightResponse() => resultFromWsResponse(responseTwo)
          case ErrorResponse(msg) => BadRequest(toJson(Map("errorMsg" -> responseOne.body)))
        }
      }
    RequestResponse
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
