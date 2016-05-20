/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.db

import org.genivi.sota.core.{DatabaseSpec, DefaultPatience, UpdateResourcesDatabaseSpec}
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}

import scala.concurrent.ExecutionContext

class UpdateSpecsSpec extends FunSuite
  with ShouldMatchers
  with UpdateResourcesDatabaseSpec
  with ScalaFutures
  with DatabaseSpec
  with DefaultPatience
   {

  import UpdateSpecs._

  implicit val ec = ExecutionContext.global
}
