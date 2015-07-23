package org.genivi.webserver.controllers

import play.api._
import play.api.mvc._

import play.api.libs.json.Json._

import javax.inject.Inject
import play.api.mvc._
import play.api.libs.ws._
import scala.concurrent.Future

class Application @Inject() (ws: WSClient) extends Controller {

  val coreHost = Play.current.configuration.getString("core.host").get
  val corePort = Play.current.configuration.getString("core.port").get
  val resolverHost = Play.current.configuration.getString("resolver.host").get
  val resolverPort = Play.current.configuration.getString("resolver.port").get
  val protocol = "http://"
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def index = Action {
    Ok(views.html.main())
  }

  def apiProxy (path: String) = Action.async(parse.json) { request =>
    val RequestResponse: Future[Result] = for {
      responseOne <- ws.url(protocol + coreHost + ":" + corePort + request.path).post(request.body)
      responseTwo <- ws.url(protocol + resolverHost + ":" + resolverPort + request.path).post(request.body)
    } yield

    if(responseTwo.status == Results.Ok) {
      Ok(responseTwo.body)
    } else {
      BadRequest(toJson(Map("errorMsg" -> responseTwo.body)))
    }
    RequestResponse
  }

  def installCampaign = Action.async(parse.json) { request =>
    ws.url(protocol + coreHost + ":" + corePort + request.path).post(request.body).map { response =>
      if(response.status == Results.Ok) {
        Ok(response.body)
      } else {
        BadRequest(toJson(Map("errorMsg" -> response.body)))
      }
    }
  }

}
