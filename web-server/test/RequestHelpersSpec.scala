import org.scalatestplus.play._
import org.genivi.webserver.requesthelpers.RequestHelper._

class RequestHelpersSpec extends PlaySpec {

  "isSuccessfulStatusCode" must {
    "return true for status code 200" in {
      val result = isSuccessfulStatusCode(200)
      result mustBe true
    }

    "return true for status code 299" in {
      val result = isSuccessfulStatusCode(299)
      result mustBe true
    }

    "return false for status code 300" in {
      val result = isSuccessfulStatusCode(300)
      result mustBe false
    }

    "return false for status code 199" in {
      val result = isSuccessfulStatusCode(199)
      result mustBe false
    }
  }
}
