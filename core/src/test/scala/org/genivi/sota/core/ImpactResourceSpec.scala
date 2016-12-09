/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.core

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.genivi.sota.core.db.BlacklistedPackages
import org.genivi.sota.data.{Namespace, PackageId, Uuid}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}
import org.genivi.sota.http.NamespaceDirectives._
import org.genivi.sota.marshalling.CirceMarshallingSupport._

import scala.concurrent.Future

class ImpactResourceSpec
  extends FunSuite
    with ScalatestRouteTest
    with DatabaseSpec
    with ShouldMatchers
    with ScalaFutures
    with DefaultPatience
    with Generators
    with LongRequestTimeout
    with UpdateResourcesDatabaseSpec {

  implicit val _db = db
  implicit val _ec = system.dispatcher

  def fakeExternalResolver(affected: Map[Uuid, Seq[PackageId]] = Map.empty) = new FakeExternalResolver() {
    override def affectedDevices(namespace: Namespace, packageIds: Set[PackageId]): Future[Map[Uuid, Seq[PackageId]]] =
      Future.successful(affected)
  }

  test("calculates impact for a blacklist item") {
    val f = for {
      (pkg, device, updateSpec) <- createUpdateSpec()
      _ <- BlacklistedPackages.create(pkg.namespace, pkg.id)
    }  yield (pkg, device)

    val (pkg, device) = f.futureValue

    val affected = Map(device.uuid -> Seq(pkg.id))

    val route = new ImpactResource(defaultNamespaceExtractor, fakeExternalResolver(affected)).route

    Get("/impact/blacklist") ~> route ~> check {
      status shouldBe StatusCodes.OK
      val resp = responseAs[Map[Uuid, Seq[PackageId]]]

      resp shouldBe Map(device.uuid -> Seq(pkg.id))
    }
  }
}
