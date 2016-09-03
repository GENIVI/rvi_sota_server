package Authentication
/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
import play.api.test.Helpers._
import org.scalatestplus.play._
import play.api.libs.ws.WSClient

/**
 * Test routing of users based on whether they have been authenticated
 */
class LoginFunTest extends PlaySpec with OneServerPerSuite {

  val wsClient = app.injector.instanceOf[WSClient]

  "redirect users to login page" in {
    val response = await(wsClient.url(s"http://localhost:$port/").get())
    response.status mustBe OK
    response.body must include("Sign in")
  }

  "refuse incorrect passwords" in {
    val response = await(wsClient.url(s"http://localhost:$port/authenticate").withFollowRedirects(true).post(Map(
    "email" -> Seq("admin@genivi.org"),
    "password" -> Seq("invalidpassword"))))
    response.status mustBe BAD_REQUEST
    response.body must include("Sign in")
  }

  "redirect logins to index page" in {
    val response = await(wsClient.url(s"http://localhost:$port/authenticate").post(Map(
      "email" -> Seq("genivi"),
      "password" -> Seq("genivirocks!"))))
    response.status mustBe OK
    response.body must include("SOTA")
  }
}
