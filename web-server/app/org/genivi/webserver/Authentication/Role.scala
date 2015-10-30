/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.webserver.Authentication

import play.api.libs.json.{JsString, JsValue, JsSuccess, Format}

/**
 * Roles for accounts
 */
object Role extends Enumeration {
  type Role = Value
  val USER = Value

  implicit val enumFormat = new Format[Role] {
    def reads(json: JsValue) = JsSuccess(Role.withName(json.as[String]))
    def writes(role: Role) = JsString(role.toString)
  }
}
