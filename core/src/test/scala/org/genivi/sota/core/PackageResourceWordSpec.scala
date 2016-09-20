/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.MalformedQueryParamRejection
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
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
import org.genivi.sota.data.PackageId
import org.genivi.sota.http.NamespaceDirectives

import scala.concurrent.duration._
import org.genivi.sota.data.Namespace
import org.genivi.sota.messaging.Messages.PackageCreated
import org.genivi.sota.messaging.{MessageBus, MessageBusPublisher}
import org.scalatest.concurrent.PatienceConfiguration

/**
 * WordSpec tests for Package REST actions
 */
class PackageResourceWordSpec extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with ShouldMatchers
  with DatabaseSpec
  with PatienceConfiguration
  with DefaultPatience
  with BeforeAndAfterAll {

  import io.circe.generic.auto._
  import CirceMarshallingSupport._
  import NamespaceDirectives._

  val externalResolverClient = new FakeExternalResolver()

  implicit val routeTimeout: RouteTestTimeout = RouteTestTimeout(20.second)

  lazy val messageBusPublisher = MessageBusPublisher.ignore

  lazy val service = new PackagesResource(externalResolverClient, db, messageBusPublisher, defaultNamespaceExtractor)

  val testPackagesParams = List(
    ("default", "vim", "7.0.1", UUID.randomUUID()),
    ("default", "vim", "7.1.1", UUID.randomUUID()),
    ("default", "go", "1.4.0", UUID.randomUUID()),
    ("default", "go", "1.5.0", UUID.randomUUID()),
    ("default", "scala", "2.11.0", UUID.randomUUID()))
  val testPackages:List[DataPackage] = testPackagesParams.map { pkg =>
    DataPackage(Namespace(pkg._1), pkg._4, PackageId(Refined.unsafeApply(pkg._2), Refined.unsafeApply(pkg._3)),
                Uri("www.example.com"), 123, "123", None, None, None)
  }

  override def beforeAll() {
    super.beforeAll()
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
    super.beforeAll()
  }
}
