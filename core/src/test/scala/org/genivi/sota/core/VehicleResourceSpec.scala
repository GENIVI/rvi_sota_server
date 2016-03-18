/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.util.UUID

import akka.http.scaladsl.unmarshalling.Unmarshaller._

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._

import akka.http.scaladsl.testkit.ScalatestRouteTest
import eu.timepit.refined.api.Refined
import io.circe.generic.auto._
import org.genivi.sota.core.db.{Vehicles, UpdateSpecs, Packages, UpdateRequests}
import org.genivi.sota.marshalling.CirceMarshallingSupport
import CirceMarshallingSupport._
import org.genivi.sota.core.data._
import org.genivi.sota.core.rvi.JsonRpcRviClient
import org.genivi.sota.core.jsonrpc.{JsonRpcDirectives, ErrorResponse, HttpTransport}
import org.scalatest.concurrent.ScalaFutures

import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}
import slick.driver.MySQLDriver.api._

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._

/**
 * Spec tests for vehicle REST actions
 */
class VehicleResourceSpec extends PropSpec with PropertyChecks
  with Matchers
  with ScalatestRouteTest
  with ScalaFutures
  with JsonRpcDirectives
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

  val updateSpecGen: Gen[(Package, Vehicle, UpdateSpec)] = for {
    smallSize ← Gen.chooseNum(1024, 1024 * 10)
    packageModel ← PackageGen.map(_.copy(size = smallSize.toLong))
    packageWithUri = Generators.generatePackageData(packageModel)
    vehicle ← Vehicle.genVehicle
    updateRequest ← updateRequestGen(PackageIdGen).map(_.copy(packageId = packageWithUri.id))
  } yield {
    val updateSpec = UpdateSpec(updateRequest, vehicle.vin,
      UpdateStatus.Pending, List(packageWithUri).toSet)

    (packageWithUri, vehicle, updateSpec)
  }

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


  val VehicleWithIllegalVin : Gen[Vehicle] = Gen.oneOf( tooLongVin, tooShortVin ).map( x => Vehicle( Refined.unsafeApply(x) ) )

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

  property("GET to download file returns the file contents") {
    forAll(updateSpecGen) { case (packageModel, vehicle: Vehicle, updateSpec: UpdateSpec) ⇒
      val url = Uri.Empty.withPath(BasePath / vehicle.vin.get / "updates" / updateSpec.request.id.toString / "download")

      val dbOps = DBIO.seq(
        Vehicles.create(vehicle),
        Packages.create(packageModel),
        UpdateRequests.persist(updateSpec.request),
        UpdateSpecs.persist(updateSpec)
      )

      val f = db.run(dbOps)

      whenReady(f) { _ ⇒
        Get(url) ~> service.route ~> check {
          status shouldBe StatusCodes.OK

          responseEntity.contentLengthOption should contain(packageModel.size)
        }
      }
    }
  }

  property("GET returns 404 if there is no package with the given id") {
    forAll { (vehicle: Vehicle) ⇒
      val uuid = UUID.randomUUID()
      val url = Uri.Empty.withPath(BasePath / vehicle.vin.get / "updates" / uuid.toString)

      Get(url) ~> service.route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[String] should include("Package not found")
      }
    }
  }

  property("GET update requests for a vehicle returns a list of UUIDS") {
    forAll(updateSpecGen) { case (packageModel, vehicle: Vehicle, updateSpec: UpdateSpec) ⇒
      val url = Uri.Empty.withPath(BasePath / vehicle.vin.get / "updates")

      val dbOps = DBIO.seq(
        Vehicles.create(vehicle),
        Packages.create(packageModel),
        UpdateRequests.persist(updateSpec.request),
        UpdateSpecs.persist(updateSpec)
      )

      val f = db.run(dbOps)

      whenReady(f) { _ ⇒
        Get(url) ~> service.route ~> check {
          status shouldBe StatusCodes.OK
          responseAs[List[UUID]] shouldNot be(empty)
          responseAs[List[UUID]] should be(List(updateSpec.request.id))
        }
      }
    }
  }

  override def afterAll() {
    system.terminate()
    db.close()
  }
}
