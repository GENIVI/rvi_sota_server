/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import CirceMarshallingSupport._
import org.genivi.sota.core.db.Vehicles
import org.genivi.sota.core.rvi.JsonRpcRviClient
import org.genivi.sota.core.jsonrpc.HttpTransport
import org.genivi.sota.data.Vehicle
import org.scalatest.BeforeAndAfterAll
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Await
import slick.driver.MySQLDriver.api._


/**
 * WordSpec for VIN REST actions
 */
class VinResourceWordSpec extends WordSpec
    with Matchers
    with ScalatestRouteTest
    with BeforeAndAfterAll {

  val databaseName = "test-database"
  val db = Database.forConfig(databaseName)

  val rviUri = Uri(system.settings.config.getString( "rvi.endpoint" ))
  val serverTransport = HttpTransport( rviUri )
  implicit val rviClient = new JsonRpcRviClient( serverTransport.requestTransport, system.dispatcher)

  lazy val service = new VehiclesResource(db, rviClient)

  val testVins = List("12345678901234500", "1234567WW0123AAAA", "123456789012345WW")

  override def beforeAll() : Unit = {
    TestDatabase.resetDatabase( databaseName )
    import scala.concurrent.duration._
    Await.ready(
      db.run( DBIO.seq( testVins.map( v => Vehicles.create(Vehicle(Refined.unsafeApply(v)))): _*) ), 2.seconds
    )
  }

  val VinsUri  = Uri( "/vehicles" )

  "Vin resource" should {
    "list resources on GET request" in {

      Get( VinsUri ) ~> service.route ~> check {
        assert(status === StatusCodes.OK)
        val vins = responseAs[Seq[Vehicle]]
        assert(vins.nonEmpty)
        assert(vins.exists(v => v.vin === Refined.unsafeApply("12345678901234500")))
        assert(vins.length === 3)
      }
    }
    "list resource on GET :vin request" in {
      Get(VinsUri + "/12345678901234500") ~> service.route ~> check {
        assert(status === StatusCodes.OK)
      }
    }
    "return a 404 on GET :vin that doesn't exist" in {
      Get(VinsUri + "/123456789N0TTHERE") ~> service.route ~> check {
        assert(status === StatusCodes.NotFound)
      }
    }
    "return a list of packages installed on a vin" in {
      Get(VinsUri + "/BLAHV1N0123456789/queued") ~> service.route ~> check {
        assert(status === StatusCodes.OK)
      }
    }
    "initiate a getpackages message to a vin" in {
      Put(VinsUri + "/V1234567890123456/sync") ~> service.route ~> check {
        assert(status === StatusCodes.NoContent)
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
    "delete a vin" in {
      Delete( VinsUri + "/123456789012345WW") ~> service.route ~> check {
        assert(status === StatusCodes.OK)
      }
    }
    "return a 404 when deleting a non existing vin" in {
      Delete( VinsUri + "/123456789N0TTHERE") ~> service.route ~> check {
        assert(status === StatusCodes.NotFound)
      }
    }
  }

  override def afterAll() : Unit = {
    system.terminate()
    db.close()
  }

}
