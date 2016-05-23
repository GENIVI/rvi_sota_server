package org.genivi.sota.web.auth

import org.genivi.webserver.Authentication.{ AuthenticationFailed, LdapAuth, SearchFailed }
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{ OneAppPerSuite, OneServerPerSuite, PlaySpec }


class LdapLoginSpec extends PlaySpec
    with OneAppPerSuite with ScalaFutures {

  val authN = app.injector.instanceOf[LdapAuth]

  "Bind login" should {
    "fail if user is not found" in {
      authN.authenticate.apply("nobody", "password").failed.futureValue must matchPattern {
        case SearchFailed(_, _, 0) =>
      }
    }

    "fail if wrong password is provided" in {
      authN.authenticate.apply("baeldung", "passwordiswrong").failed.futureValue must matchPattern {
        case AuthenticationFailed(_, Some(_)) =>
      }
    }

    "return account if user authenticated" in {
      authN.authenticate.apply("baeldung", "password").futureValue
    }
  }

}
