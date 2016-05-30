/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import io.circe.syntax._
import org.genivi.sota.core.data.{VehicleStatus, VehicleUpdateStatus}
import org.genivi.sota.core.jsonrpc.HttpTransport
import org.genivi.sota.core.rvi._
import org.genivi.sota.data.Namespaces
import org.genivi.sota.data.Vehicle
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.scalacheck.Gen
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}
import slick.driver.MySQLDriver.api._


/**
 * Spec tests for vehicle REST actions
 */
class VehicleResourceSpec extends PropSpec
  with PropertyChecks
  with Matchers
  with ScalatestRouteTest
  with ScalaFutures
  with DatabaseSpec
  with VehicleDatabaseSpec
  with Namespaces {

  import CirceMarshallingSupport._
  import Generators._
  import org.genivi.sota.data.VehicleGenerators._
  import org.genivi.sota.data.PackageIdGenerators._

  val rviUri = Uri(system.settings.config.getString( "rvi.endpoint" ))
  val serverTransport = HttpTransport( rviUri )
  implicit val rviClient = new JsonRpcRviClient( serverTransport.requestTransport, system.dispatcher)

  val fakeResolver = new FakeExternalResolver()

  lazy val service = new VehiclesResource(db, rviClient, fakeResolver)

  val BasePath = Path("/vehicles")

  implicit val patience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  implicit val _db = db

  def resourceUri( pathSuffix : String ) : Uri = {
    Uri.Empty.withPath(BasePath / pathSuffix)
  }

  def vehicleUri(vin: Vehicle.Vin)  = Uri.Empty.withPath( BasePath / vin.get )

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


  val VehicleWithIllegalVin : Gen[Vehicle] = for {
    vin <- Gen.oneOf(tooLongVin, tooShortVin)
  } yield Vehicle(defaultNs, Refined.unsafeApply(vin))

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

  property("search with status=true returns current status for a vehicle") {
    whenReady(createVehicle()) { vin =>
      val url = Uri.Empty
        .withPath(BasePath)
        .withQuery(Uri.Query("status" -> "true"))

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.OK
        val parsedResponse = responseAs[Seq[VehicleUpdateStatus]].headOption

        parsedResponse.flatMap(_.lastSeen) shouldNot be(defined)
        parsedResponse.map(_.status) should contain(VehicleStatus.NotSeen)
      }
    }
  }
}
