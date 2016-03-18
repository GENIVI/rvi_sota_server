package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.util.FastFuture
import io.circe.Encoder
import java.util.UUID
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.core.data.UpdateRequest
import org.genivi.sota.core.db.UpdateRequests
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.PropertyChecks
import scala.concurrent.Future
import slick.jdbc.JdbcBackend.Database
import io.circe.generic.auto._
import CirceMarshallingSupport._
import org.genivi.sota.core.rvi.{ServerServices, RviClient}

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
    implicit val rviClient = new RviClient {

      import org.joda.time.DateTime
      override def sendMessage[A](service: String, message: A, expirationDate: DateTime)
        (implicit encoder: Encoder[A] ) : Future[Int] = FastFuture.successful(0)

    }

    val resource = new UpdateRequestsResource(db, externalResolverClient,
                                              new UpdateService( ServerServices("", "", "", "")))
  }

  import UpdateRequest._

  property("Update requests can be listed")  {
    new Service() {
      forAll(Gen.listOf(updateRequestGen(PackageIdGen))) { (requests: Seq[UpdateRequest]) =>
        // TODO: I don't think this test is running, we just create a future and never wait for it's result
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
