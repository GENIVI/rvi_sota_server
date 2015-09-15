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
import eu.timepit.refined._
import org.genivi.sota.core.data.PackageId
import org.genivi.sota.core.data.{ Package => DataPackage }
import org.genivi.sota.core.db.Packages
import org.scalatest.BeforeAndAfterAll
import org.scalatest.{WordSpec, Matchers}
import org.scalatest.ShouldMatchers
import scala.concurrent.Await
import slick.driver.MySQLDriver.api._
import spray.json.DefaultJsonProtocol._


class PackageResourceWordSpec extends WordSpec
    with Matchers
    with ScalatestRouteTest
    with ShouldMatchers
    with BeforeAndAfterAll {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  val databaseName = "test-database"

  val config = system.settings.config
  val externalResolverClient = new DefaultExternalResolverClient( Uri(config.getString("resolver.baseUri")) )
  val db = Database.forConfig(databaseName)
  lazy val service = new PackagesResource(externalResolverClient, db)

  val testPackagesParams = List(("vim", "7.0.1"), ("vim", "7.1.1"), ("go", "1.4.0"), ("go", "1.5.0"), ("scala", "2.11.0"))
  val testPackages:List[DataPackage] = testPackagesParams.map { pkg =>
    DataPackage(PackageId(Refined(pkg._1), Refined(pkg._2)), Uri("www.example.com"), 123, "123", None, None)
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
        assert(packages.filter(pkg => pkg.id === PackageId(Refined("scala"), Refined("2.11.0"))).nonEmpty)
        assert(packages.length === 5)
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
      println(PackagesUri + "?regex=)")
      Get(PackagesUri + "?regex=)" ) ~> service.route ~> check {
        rejection shouldBe a [MalformedQueryParamRejection]
        assert(rejection === MalformedQueryParamRejection("regex", "Regex predicate failed: Unmatched closing \')\'\n)", None))
      }
    }
  }

  override def afterAll() {
    system.shutdown()
    db.close()
  }

}
