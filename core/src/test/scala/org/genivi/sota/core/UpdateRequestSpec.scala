package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.util.FastFuture
import io.circe.Encoder
import io.circe.generic.auto._
import java.util.UUID
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.core.db.UpdateRequests
import org.genivi.sota.core.rvi.{ServerServices, RviConnectivity}
import org.genivi.sota.core.transfer.DefaultUpdateNotifier
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.PropertyChecks
import scala.concurrent.Future
import slick.jdbc.JdbcBackend.Database
import CirceMarshallingSupport._

/**
 * Spec tests for update-request REST actions
 */
class UpdateRequestSpec extends PropSpec with PropertyChecks with Matchers with Generators with ScalatestRouteTest {

  val UpdatesPath = Path / "updates"

  val databaseName = "test-database"

  val config = system.settings.config
  val externalResolverClient = new DefaultExternalResolverClient(
    Uri(config.getString("resolver.baseUri")),
    Uri(config.getString("resolver.resolveUri")),
    Uri(config.getString("resolver.packagesUri")),
    Uri(config.getString("resolver.vehiclesUri"))
  )
  val db = Database.forConfig(databaseName)

  override def beforeAll() : Unit = {
    TestDatabase.resetDatabase( databaseName )
  }

  class Service(implicit system: ActorSystem) {
    implicit val log = Logging(system, "updateRequestsSpec")
    implicit val connectivity = DefaultConnectivity

    val resource = new UpdateRequestsResource(db, externalResolverClient,
                                              new UpdateService(DefaultUpdateNotifier))
  }

  import UpdateRequest._

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

  override def afterAll() : Unit = {
    system.terminate()
    db.close()
  }
}
