package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.core.db.UpdateRequests
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.PropertyChecks
import scala.concurrent.Future
import slick.jdbc.JdbcBackend.Database
import io.circe.generic.auto._
import org.genivi.sota.CirceSupport._

class UpdateRequestSpec extends PropSpec with PropertyChecks with Matchers with Generators with ScalatestRouteTest {

  val UpdatesPath = Path / "updates"

  val databaseName = "test-database"

  val config = system.settings.config
  val externalResolverClient = new DefaultExternalResolverClient(
    Uri(config.getString("resolver.baseUri")),
    Uri(config.getString("resolver.resolveUri")),
    Uri(config.getString("resolver.packagesUri")) )
  val db = Database.forConfig(databaseName)

  override def beforeAll {
    TestDatabase.resetDatabase( databaseName )
  }

  class Service(implicit system: ActorSystem) {
    implicit val log = Logging(system, "updateRequestsSpec")
    val resource = new UpdateRequestsResource(db, externalResolverClient, new UpdateService())
  }

  import UpdateRequest._
  import spray.json.DefaultJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  property("Update requests can be listed")  {
    new Service() {
      forAll(Gen.listOf(updateRequestGen(PackageIdGen))) { (requests: Seq[UpdateRequest]) =>
        Future.sequence(requests.map(r => db.run(UpdateRequests.persist(r)))).map { _ =>
          Get( Uri(path = UpdatesPath) ) ~> resource.route ~> check {
            status shouldBe StatusCodes.OK
            responseAs[Seq[UpdateRequest]] shouldBe requests
          }
        }
      }
    }
  }

  override def afterAll() {
    system.shutdown()
    db.close()
  }
}
