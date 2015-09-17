/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import eu.timepit.refined.Refined
import io.circe.generic.auto._
import org.genivi.sota.CirceSupport._
import org.genivi.sota.core.data.Vehicle
import org.genivi.sota.core.db.Vehicles
import org.scalatest.BeforeAndAfterAll
import org.scalatest.{WordSpec, Matchers}
import scala.concurrent.Await
import slick.driver.MySQLDriver.api._


class VinResourceWordSpec extends WordSpec
    with Matchers
    with ScalatestRouteTest
    with BeforeAndAfterAll {

  val databaseName = "test-database"

  val db = Database.forConfig(databaseName)
  lazy val service = new VehiclesResource(db)

  val testVins = List("12345678901234500", "1234567WW0123AAAA", "123456789012345WW")

  override def beforeAll {
    TestDatabase.resetDatabase( databaseName )
    import scala.concurrent.duration._
    Await.ready( db.run( DBIO.seq( testVins.map( v => Vehicles.create(Vehicle(Refined(v)))): _*) ), 2.seconds )
  }

  val VinsUri  = Uri( "/vehicles" )

  "Vin resource" should {
    "list resources on GET request" in {

      Get( VinsUri ) ~> service.route ~> check {
        assert(status === StatusCodes.OK)
        val vins = responseAs[Seq[Vehicle]]
        assert(vins.nonEmpty)
        assert(vins.filter(v => v.vin === Refined("12345678901234500")).nonEmpty)
        assert(vins.length === 3)
      }
    }
    "filter list of vins by regex 'WW'" in {
      Get(VinsUri + "?regex=WW") ~> service.route ~> check {
        assert(status === StatusCodes.OK)
        val vins = responseAs[Seq[Vehicle]]
        assert(vins.length === 2)
      }
    }
    "filter list of vins by regex 'WW$'" in {
      Get( VinsUri + "?regex=WW$" ) ~> service.route ~> check {
        assert(status === StatusCodes.OK)
        val vins = responseAs[Seq[Vehicle]]
        assert(vins.length === 1)
      }
    }

  }

  override def afterAll() {
    system.shutdown()
    db.close()
  }

}
