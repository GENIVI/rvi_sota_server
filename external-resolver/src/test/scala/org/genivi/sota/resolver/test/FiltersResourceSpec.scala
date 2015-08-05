package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import org.genivi.sota.resolver.types.Filter


class FiltersResourceSpec extends ResourceWordSpec {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  "Filters resource" should {

    "create a new resource on POST request" in {
      Post(FiltersUri, Filter(None, "myfilter", "true")) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }
}