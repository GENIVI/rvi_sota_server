import org.genivi.webserver.requesthelpers.{ErrorResponse, RightResponse, LeftResponse}
import org.scalatestplus.play._
import org.genivi.webserver.requesthelpers.RequestHelper._

class RequestHelpersSpec extends PlaySpec {

  "chooseResponse" must {
    "Pick the left one if the other is 404" in {
      val result = chooseResponse(200, 404)
      result mustBe LeftResponse()
    }

    "Pick the right one if the other is 404" in {
      val result = chooseResponse(404, 200)
      result mustBe RightResponse()
    }

    "Pick either if both succeed" in {
      val result = chooseResponse(200, 200)
      result mustBe LeftResponse() // This is a temporary workaround
    }

    "Return an error if neither are 404" in {
      val result = chooseResponse(500, 200)
      result mustBe a[ErrorResponse]
    }

    "Either is fine for a 204 No Content response" in {
      val result = chooseResponse(204, 204)
      result mustBe LeftResponse()
    }
  }
}
