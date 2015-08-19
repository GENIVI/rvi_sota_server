package org.genivi.sota.core

import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model.{StatusCodes, HttpEntity}
import akka.http.scaladsl.server.PathMatchers
import akka.http.scaladsl.testkit.ScalatestRouteTest
import java.io.File
import org.genivi.sota.core.data.{Vehicle, PackageId, Package}
import org.scalatest.{ PropSpec, Matchers }
import org.scalatest.prop.PropertyChecks
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.Multipart
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Future

/**
 * Created by vladimir on 14/08/15.
 */
class PackageUploadSpec extends PropSpec with PropertyChecks with Matchers with Generators with ScalatestRouteTest {

  val PackagesPath = Path / "api" / "v1" / "packages"

  val databaseName = "test-database"

  val db = Database.forConfig(databaseName)

  val resolver = new ExternalResolverClient {
    override def putPackage(packageId: PackageId, description: Option[String], vendor: Option[String]): Future[Unit] = Future.successful(())

    override def resolve(packageId: PackageId): Future[Map[Vehicle, Set[PackageId]]] = ???
  }

  lazy val service = new WebService(resolver, db)

  override def beforeAll {
    TestDatabase.resetDatabase( databaseName )
  }

  def toBodyPart(name : String)(x: String) = BodyPart.Strict(name, HttpEntity( x ) )

  property("Package can be uploaded using POST request") {
    forAll { (pckg: Package) =>
      val filePart = BodyPart.fromFile( "file", ContentType(MediaTypes.`application/x-redhat-package-manager`), new File("""packages/ghc-7.6.3-18.3.el7.x86_64.rpm"""))
      val parts = filePart :: List( pckg.description.map( toBodyPart("description") ), pckg.vendor.map( toBodyPart("vendor") ) ).filter(_.isDefined).map(_.get)
      val form = Multipart.FormData( parts: _* )

      Put( Uri( path = PackagesPath / pckg.id.name.get / pckg.id.version.get ), form ) ~> service.route ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }
  }

  override def afterAll() {
    system.shutdown()
    db.close()
  }
}
