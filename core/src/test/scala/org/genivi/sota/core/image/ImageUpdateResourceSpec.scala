/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */

package org.genivi.sota.core.image

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.genivi.sota.core.{DatabaseSpec, DefaultPatience, ImageGenerators}
import org.genivi.sota.core.db.image.ImageRepositorySupport
import org.scalatest.{FunSuite, ShouldMatchers}
import org.scalatest.concurrent.ScalaFutures
import org.genivi.sota.http.NamespaceDirectives.defaultNamespaceExtractor

class ImageUpdateResourceSpec extends FunSuite
  with ScalatestRouteTest
  with DatabaseSpec
  with ShouldMatchers
  with ScalaFutures
  with ImageGenerators
  with DefaultPatience
  with ImageRepositorySupport {

  implicit val _db = db

  val route = new ImageUpdateResource(defaultNamespaceExtractor).route

  test("fail") {
    fail("wat")
  }
}
