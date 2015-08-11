/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.webserver.Authentication

import java.io.FileNotFoundException
import org.mindrot.jbcrypt.BCrypt
import play.api.Play
import play.api.libs.json._

//id is simply the index of the account in the list
case class Account(email: String, password: String, name: String, role: Role.Role)

class AccountManager {

  //this needs to be lazy so it isn't evaluated until after the application is running,
  //ensuring we don't get an exception when running unit tests
  lazy val passwordFile = Play.current.configuration.getString("file.password").get

  lazy val accounts = readAccounts()

  implicit val accountFormat = Json.format[Account]

  def authenticate(email: String, password: String): Option[Account] = {
    findById(email).filter { account => BCrypt.checkpw(password, account.password) }
  }

  def findById(email: String): Option[Account] = {
    accounts.find{account => account.email.equals(email)}
  }

  def readAccounts() : List[Account] = {
    val rawData = scala.io.Source.fromFile(Play.current.path.getCanonicalPath + "/" + passwordFile).mkString
    val accountsResult = Json.parse(rawData)
    accountsResult.validate[List[Account]] match {
      case JsSuccess(acc, _) => acc
      case _ => throw new FileNotFoundException("Password file not found")
    }
  }

}
