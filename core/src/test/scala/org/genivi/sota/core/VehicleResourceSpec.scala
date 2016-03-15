/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport
import CirceMarshallingSupport._
import org.genivi.sota.core.rvi.JsonRpcRviClient
import org.genivi.sota.core.jsonrpc.HttpTransport
import org.genivi.sota.data.Vehicle
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}
import slick.driver.MySQLDriver.api._

/**
 * Spec tests for vehicle REST actions
 */
class VehicleResourceSpec extends PropSpec with PropertyChecks
    with Matchers
    with ScalatestRouteTest
    with BeforeAndAfterAll {

  val databaseName = "test-database"
  val db = Database.forConfig(databaseName)

  val rviUri = Uri(system.settings.config.getString( "rvi.endpoint" ))
  val serverTransport = HttpTransport( rviUri )
  implicit val rviClient = new JsonRpcRviClient( serverTransport.requestTransport, system.dispatcher)

  lazy val service = new VehiclesResource(db, rviClient)

  override def beforeAll {
    TestDatabase.resetDatabase( databaseName )
  }

  val BasePath = Path("/vehicles")

  def resourceUri( pathSuffix : String ) : Uri = {
    Uri.Empty.withPath(BasePath / pathSuffix)
  }

  def vehicleUri(vin: Vehicle.Vin)  = Uri.Empty.withPath( BasePath / vin.get )

  import Generators._
  import org.genivi.sota.data.VehicleGenerators._

  property( "create new vehicle" ) {
    forAll { (vehicle: Vehicle) =>
      Put( vehicleUri(vehicle.vin), vehicle ) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }
  }

  val tooLongVin = for {
    n <- Gen.choose(18, 100)
    vin <- Gen.listOfN(n, Gen.alphaNumChar)
  } yield vin.mkString

  val tooShortVin = for {
    n <- Gen.choose(1, 16)
    vin <- Gen.listOfN(n, Gen.alphaNumChar)
  } yield vin.mkString


  val VehicleWithIllegalVin : Gen[Vehicle] = Gen.oneOf( tooLongVin, tooShortVin )
      .map( x => Vehicle( Refined.unsafeApply(x) ) )

  property( "reject illegal vins" ) {
    forAll( VehicleWithIllegalVin ) { vehicle =>
      Put( vehicleUri(vehicle.vin), vehicle ) ~> Route.seal(service.route) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }
  }

  property( "Multiple PUT requests with the same vin are allowed" ) {
    forAll { (vehicle: Vehicle ) =>
      Put( vehicleUri(vehicle.vin), vehicle ) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent
      }

      Put( vehicleUri(vehicle.vin), vehicle ) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }
  }

  override def afterAll() {
    system.terminate()
    db.close()
  }

}
