/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.genivi.sota.core.db.BlacklistedPackages
import org.genivi.sota.data.{Device, PackageId}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}
import org.genivi.sota.http.NamespaceDirectives._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.CirceInstances._
import io.circe.generic.auto._

class ImpactResourceSpec
  extends FunSuite
    with ScalatestRouteTest
    with DatabaseSpec
    with ShouldMatchers
    with ScalaFutures
    with DefaultPatience
    with Generators
    with UpdateResourcesDatabaseSpec {

  implicit val _db = db
  implicit val _ec = system.dispatcher

  val route = new ImpactResource(defaultNamespaceExtractor).route

  test("calculates impact for a blacklist item") {
    val f = for {
      (pkg, device, updateSpec) <- createUpdateSpec()
      _ <- BlacklistedPackages.create(pkg.namespace, pkg.id)
    }  yield (pkg, device)

    val (pkg, device) = f.futureValue

    Get("/impact/blacklist") ~> route ~> check {
      status shouldBe StatusCodes.OK
      val resp = responseAs[Seq[(Device.Id, PackageId)]]

      resp should contain((device.id, pkg.id))
    }
  }
}
