package org.genivi.sota.core

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.server.PathMatchers
import akka.http.scaladsl.testkit.ScalatestRouteTest
import java.io.File
import org.genivi.sota.core.data.{Vehicle, PackageId, Package}
import org.genivi.sota.rest.ErrorRepresentation
import org.scalatest.{ PropSpec, Matchers }
import org.scalatest.prop.PropertyChecks
import akka.http.scaladsl.model.Uri.Path
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Future

/**
 * Created by vladimir on 14/08/15.
 */
class PackageUploadSpec extends PropSpec with PropertyChecks with Matchers with Generators with ScalatestRouteTest {

  val PackagesPath = Path / "packages"

  val databaseName = "test-database"

  val db = Database.forConfig(databaseName)

  override def beforeAll {
    TestDatabase.resetDatabase( databaseName )
  }

  class Service(resolverResult: Future[Unit] ) {
    val resolver = new ExternalResolverClient {
      override def putPackage(packageId: PackageId, description: Option[String], vendor: Option[String]): Future[Unit] = resolverResult

      override def resolve(packageId: PackageId): Future[Map[Vehicle, Set[PackageId]]] = ???
    }

    val resource = new PackagesResource(resolver, db)
  }

  def toBodyPart(name : String)(x: String) = BodyPart.Strict(name, HttpEntity( x ) )

  private[this] def mkRequest( pckg: Package ) : HttpRequest = {
    val filePart = BodyPart.fromFile( "file", ContentType(MediaTypes.`application/x-redhat-package-manager`), new File("""packages/ghc-7.6.3-18.3.el7.x86_64.rpm"""))
    val parts = filePart :: List( pckg.description.map( toBodyPart("description") ), pckg.vendor.map( toBodyPart("vendor") ) ).filter(_.isDefined).map(_.get)
    val form = Multipart.FormData( parts: _* )

    Put( Uri( path = PackagesPath / pckg.id.name.get / pckg.id.version.get ), form )
  }

  property("Package can be uploaded using POST request")  {
    new Service( Future.successful(())) {
      forAll { (pckg: Package) =>
        mkRequest(pckg) ~> resource.route ~> check {
          status shouldBe StatusCodes.NoContent
        }
      }
    }
  }

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  property("Returns service unavailable if request to external resolver fails") {
    new Service( Future.failed( ExternalResolverRequestFailed( StatusCodes.InternalServerError ) ) ) {
      import org.genivi.sota.CirceSupport._
      import io.circe.generic.auto._
      import akka.http.scaladsl.unmarshalling._
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

  override def afterAll() {
    system.shutdown()
    db.close()
  }
}
