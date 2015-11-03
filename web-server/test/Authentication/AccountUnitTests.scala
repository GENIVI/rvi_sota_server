package Authentication
/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
import org.genivi.webserver.Authentication.{AccountManager, Role}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import scala.language.reflectiveCalls

/**
 * Tests account authorization
 */
class AccountUnitTests extends PlaySpec with OneServerPerSuite {

  def fixture = new {
    val email = "admin@genivi.org"
    val name = "admin"
    val password = "genivirocks!"
    val role = Role.USER
    val accountManager = new AccountManager()
  }

  "Account object" must {
    "prevent users logging in with an incorrect password" in {
      val f = fixture
      f.accountManager.authenticate(f.email, "invalidPassword") mustBe None
    }
  }
}
