package org.genivi.sota.resolver.test

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

class ResolveResourceWordSpec extends ResourceWordSpec {

  "Resolve resource" should {

    "answer GET requests" in {
      Get(ResolveUri(1)) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

}
