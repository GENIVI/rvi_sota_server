import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import play.api.libs.ws.WS

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends PlaySpec with OneServerPerSuite {

  "send 404 on a bad request" in {
    val response = await(WS.url(s"http://localhost:$port/invalid").get())
    response.status mustBe (NOT_FOUND)
  }

  "render the index page" in {
    val response = await(WS.url(s"http://localhost:$port/").get())
    response.status mustBe (OK)
  }
}
