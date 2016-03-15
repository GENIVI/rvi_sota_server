/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.webserver.Authentication

import java.io.FileNotFoundException

import org.mindrot.jbcrypt.BCrypt
import play.api.Play
import play.api.libs.json._

/**
 * Case class for Account
 *
 * @param email Account email
 * @param password Account password
 * @param name Account name
 * @param role Account role
 */
case class Account(email: String, password: String, name: String, role: Role.Role)

/**
 * Class to find and authenticate accounts, and to read accounts from file.
 */
class AccountManager {

  //this needs to be lazy so it isn't evaluated until after the application is running,
  //ensuring we don't get an exception when running unit tests
  lazy val passwordFile = Play.current.configuration.getString("file.password").get

  lazy val accounts = readAccounts()

  implicit val accountFormat = Json.format[Account]

  /**
   * Authenticates an account by email and password
   *
   * @param email Account email
   * @param password Account password
   * @return Option of the authenticated account
   */
  def authenticate(email: String, password: String): Option[Account] = {
    findById(email).filter { account => BCrypt.checkpw(password, account.password) }
  }

  /**
   * Find account by email
   *
   * @param email Account email
   * @return Option of the account
   */
  def findById(email: String): Option[Account] = {
    accounts.find{account => account.email.equals(email)}
  }

  /**
   * Reads accounts from file
   *
   * @return List of accounts found in the file.
   */
  def readAccounts() : List[Account] = {
    val rawData = scala.io.Source.fromFile(Play.current.path.getCanonicalPath + "/" + passwordFile).mkString
    val accountsResult = Json.parse(rawData)
    accountsResult.validate[List[Account]] match {
      case JsSuccess(acc, _) => acc
      case _ => throw new FileNotFoundException("Password file not found")
    }
  }

}
