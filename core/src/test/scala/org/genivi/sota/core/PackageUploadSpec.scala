package org.genivi.sota.core

import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.PathMatchers
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling._
import io.circe.generic.auto._
import java.io.File
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.resolver.{ExternalResolverClient, ExternalResolverRequestFailed}
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.datatype.NamespaceDirective
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.rest.ErrorRepresentation
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, PropSpec}
import scala.concurrent.Future
import scala.concurrent.duration._
import slick.jdbc.JdbcBackend.Database


/**
 * Spec tests for package upload to Core
 */
class PackageUploadSpec extends PropSpec
  with DatabaseSpec
  with PropertyChecks
  with Matchers
  with Generators
  with ScalatestRouteTest
  with ScalaFutures {

  import NamespaceDirective._

  implicit val routeTimeout: RouteTestTimeout = RouteTestTimeout(10.second)

  val PackagesPath = Path / "packages"

  implicit val patience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  class Service(resolverResult: Future[Unit] ) {
    val resolver = new ExternalResolverClient {
      override def putPackage(namespace: Namespace, packageId: PackageId, description: Option[String], vendor: Option[String]): Future[Unit] = resolverResult

      override def resolve(namespace: Namespace, packageId: PackageId): Future[Map[Vehicle, Set[PackageId]]] = ???

      override def setInstalledPackages( vin: Vehicle.Vin, json: io.circe.Json) : Future[Unit] = ???
    }

    val resource = new PackagesResource(resolver, db, defaultNamespaceExtractor)
  }

  def toBodyPart(name : String)(x: String) = BodyPart.Strict(name, HttpEntity( x ) )

  private[this] def mkRequest( pckg: Package ) : HttpRequest = {
    val filePart = BodyPart.fromFile( "file",
      ContentType(MediaTypes.`application/x-redhat-package-manager`),
      new File("""packages/ghc-7.6.3-18.3.el7.x86_64.rpm"""))
    val parts = filePart ::
        List( pckg.description.map( toBodyPart("description") ),
          pckg.vendor.map( toBodyPart("vendor") )
        ).filter(_.isDefined).map(_.get)
    val form = Multipart.FormData( parts: _* )

    Put( Uri( path = PackagesPath / pckg.id.name.get / pckg.id.version.get ), form )
  }

  property("Package can be uploaded using PUT request")  {
    new Service( Future.successful(())) {
      forAll { (pckg: Package) =>
        mkRequest(pckg) ~> resource.route ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }
    }
  }

  property("Returns service unavailable if request to external resolver fails") {
    import CirceMarshallingSupport._
    new Service( Future.failed( ExternalResolverRequestFailed( StatusCodes.InternalServerError ) ) ) {
      forAll { (pckg: Package) =>
        mkRequest( pckg ) ~> resource.route ~> check {
          status shouldBe StatusCodes.ServiceUnavailable
          responseAs[ErrorRepresentation].code shouldBe ErrorCodes.ExternalResolverError
        }
      }
    }
  }

  property("Upsert") {
    new Service( Future.successful(())) {
      forAll { (pckg: Package) =>
        mkRequest( pckg ) ~> resource.route ~> check {
          status shouldBe StatusCodes.NoContent
        }

        mkRequest( pckg ) ~> resource.route ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }
    }
  }

  override def afterAll() : Unit = {
    system.terminate()
    db.close()
  }
}
