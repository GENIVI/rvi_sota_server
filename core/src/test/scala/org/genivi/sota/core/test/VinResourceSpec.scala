/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.test

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.genivi.sota.core.Vin
import org.scalatest.{WordSpec, Matchers}

class VinResourceSpec extends WordSpec with Matchers with ScalatestRouteTest {
  import org.genivi.sota.core.JsonProtocols._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  lazy val service = new org.genivi.sota.core.WebService 

  val BasePath = Path("/api") / "v1"

  def resourceUri( pathSuffix : String ) : Uri = {
    Uri.Empty.withPath(BasePath / pathSuffix)
  }

  val VinsUri  = resourceUri("vins")

  "Vin resource" should {
    "create a new resource on POST request" in {
      Post( VinsUri, Vin("VINOOLAM0FAU2DEEP") ) ~> service.route ~> check {
        status === 200
      }
    }
  }

}
