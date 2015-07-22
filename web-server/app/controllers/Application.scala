package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json.Json._

class Application extends Controller {

  def index = Action {
    Ok(views.html.main())
  }

  def addVin = Action(parse.json) { request =>
    (request.body \ "vin").asOpt[String].map { vinText =>
      /* VINs are 17 uppercase chars || digits */
      val vinRegex = "^[A-Z0-9]{17}$".r
      val vin = vinRegex.findFirstIn(vinText)
      if(vin.isDefined) {
        Ok("")
      } else {
        BadRequest(toJson(Map("errorMsg" -> ("Invalid VIN: " + vinText))))
      }
    }.getOrElse {
      BadRequest(toJson(Map("errorMsg" -> "Invalid request")))
    }
  }

}
