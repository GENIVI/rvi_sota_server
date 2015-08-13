/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.genivi.sota.core.data.Vehicle
import org.genivi.sota.core.db.Vehicles
import eu.timepit.refined.internal.Wrapper
import org.scalatest.BeforeAndAfterAll
import akka.http.scaladsl.model.StatusCodes
import org.scalatest.{WordSpec, Matchers}
import slick.driver.MySQLDriver.api._

import scala.concurrent.Await

class VinResourceWordSpec extends WordSpec
    with Matchers
    with ScalatestRouteTest
    with BeforeAndAfterAll {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  val databaseName = "test-database"

  val db = Database.forConfig(databaseName)
  lazy val service = new WebService (db)

  val testVins = List("12345678901234500", "1234567WW0123AAAA", "123456789012345WW")

  override def beforeAll {
    TestDatabase.resetDatabase( databaseName )
    import scala.concurrent.duration._
    Await.ready( db.run( DBIO.seq( testVins.map( v => Vehicles.create(new Vehicle(new Vehicle.IdentificationNumber(v)))): _*) ), 2.seconds )
  }

  val BasePath = Path("/api") / "v1"

  def resourceUri( pathSuffix : String ) : Uri = {
    Uri.Empty.withPath(BasePath / pathSuffix)
  }

  val VinsUri  = resourceUri("vehicles")

  "Vin resource" should {
    "list resources on GET request" in {

      Get( VinsUri ) ~> service.route ~> check {
        assert(status === StatusCodes.OK)
        val vins = responseAs[Seq[Vehicle]]
        assert(vins.nonEmpty)
        assert(vins.filter(v => v.vin === new Vehicle.IdentificationNumber("12345678901234500")).nonEmpty)
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
