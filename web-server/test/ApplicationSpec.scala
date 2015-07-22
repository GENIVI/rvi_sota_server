import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/invalid")) must beSome.which (status(_) == NOT_FOUND)
    }

    "render the index page" in new WithApplication{
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
    }

    "allow adding a new, valid vin" in new WithApplication{
      val addVin = route(FakeRequest(POST, "/addVin",
        FakeHeaders(List(("Content-Type", "application/json"))),
        """ {"vin": "TESTVIN0123456789"} """ )).get

      status(addVin) must equalTo(OK)
    }

    "reject vins with incorrect length" in new WithApplication{
      val addVin = route(FakeRequest(POST, "/addVin",
        FakeHeaders(List(("Content-Type", "application/json"))),
        """ {"vin": "TESTVIN012345678"} """ )).get

      status(addVin) must equalTo(BAD_REQUEST)
    }

    "reject vins with invalid chars" in new WithApplication{
      val addVin = route(FakeRequest(POST, "/addVin",
        FakeHeaders(List(("Content-Type", "application/json"))),
        """ {"vin": "TESTvIN0123456789"} """ )).get

      status(addVin) must equalTo(BAD_REQUEST)
    }
  }
}
