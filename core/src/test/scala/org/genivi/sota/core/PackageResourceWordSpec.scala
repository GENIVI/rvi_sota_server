/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.MalformedQueryParamRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import eu.timepit.refined.api.Refined
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.core.data.{Package => DataPackage}
import org.genivi.sota.core.db.Packages
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ShouldMatchers
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Await
import slick.driver.MySQLDriver.api._
import DataPackage._
import org.genivi.sota.core.resolver.DefaultExternalResolverClient
import org.genivi.sota.data.PackageId

/**
 * WordSpec tests for Package REST actions
 */
class PackageResourceWordSpec extends WordSpec
    with Matchers
    with ScalatestRouteTest
    with ShouldMatchers
    with BeforeAndAfterAll {

  import io.circe.generic.auto._
  import CirceMarshallingSupport._

  val databaseName = "test-database"

  val config = system.settings.config
  val externalResolverClient = new DefaultExternalResolverClient(
    Uri(config.getString("resolver.baseUri")),
    Uri(config.getString("resolver.resolveUri")),
    Uri(config.getString("resolver.packagesUri")),
    Uri(config.getString("resolver.vehiclesUri"))
  )
  val db = Database.forConfig(databaseName)
  lazy val service = new PackagesResource(externalResolverClient, db)

  val testPackagesParams = List(
    ("vim", "7.0.1"), ("vim", "7.1.1"), ("go", "1.4.0"), ("go", "1.5.0"), ("scala", "2.11.0"))
  val testPackages:List[DataPackage] = testPackagesParams.map { pkg =>
    DataPackage(PackageId(Refined.unsafeApply(pkg._1), Refined.unsafeApply(pkg._2)),
                Uri("www.example.com"), 123, "123", None, None, None)
  }

  override def beforeAll {
    TestDatabase.resetDatabase( databaseName )
    import scala.concurrent.duration._
    Await.ready( db.run( DBIO.seq( testPackages.map( pkg => Packages.create(pkg)): _*) ), 2.seconds )
  }

  val PackagesUri  = Uri("/packages")

  "Package resource" should {
    "list resources on GET request" in {

      Get( PackagesUri ) ~> service.route ~> check {
        assert(status === StatusCodes.OK)
        val packages = responseAs[Seq[DataPackage]]
        assert(packages.nonEmpty)
        assert(packages.exists { pkg =>
          pkg.id === PackageId(Refined.unsafeApply("scala"), Refined.unsafeApply("2.11.0"))
        })
        assert(packages.length === 5)
      }
    }
    "list resource on GET :package request" in {
      Get(PackagesUri + "/scala/2.11.0") ~> service.route ~> check {
        assert(status === StatusCodes.OK)
        val pkg = responseAs[DataPackage]
        assert(pkg.id === PackageId(Refined.unsafeApply("scala"), Refined.unsafeApply("2.11.0")))
      }
    }
    "filter list of packages by regex '0'" in {
      Get(PackagesUri + "?regex=0") ~> service.route ~> check {
        assert(status === StatusCodes.OK)
        val packages = responseAs[List[DataPackage]]
        assert(packages.length === 4)
      }
    }
    "filter list of packages by regex '0$'" in {
      Get(PackagesUri + "?regex=0$" ) ~> service.route ~> check {
        assert(status === StatusCodes.OK)
        val packages = responseAs[List[DataPackage]]
        assert(packages.length === 3)
      }
    }
    "returns 400 for bad request" in {
      Get(PackagesUri + "?regex=)" ) ~> service.route ~> check {
        rejection shouldBe a [MalformedQueryParamRejection]
        assert(rejection === MalformedQueryParamRejection("regex", "Regex predicate failed: Unmatched closing \')\'\n)",
          None))
      }
    }
  }

  override def afterAll() {
    system.terminate()
    db.close()
  }

}
