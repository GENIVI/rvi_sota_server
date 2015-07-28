/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.BeforeAndAfterAll
import org.scalatest.{WordSpec, Matchers}

class VinResourceSpec extends WordSpec
    with TestDatabase
    with Matchers
    with ScalatestRouteTest
    with BeforeAndAfterAll {

  import org.genivi.sota.core.JsonProtocols._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  lazy val service = new WebService (db)

  override def beforeAll {
    resetDatabase
  }

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
