package org.genivi.sota.core

import java.util.UUID

import akka.event.Logging
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.Encoder
import io.circe.generic.auto._
import org.genivi.sota.core.resolver.{ConnectivityClient, DefaultConnectivity}
import org.genivi.sota.core.data.UpdateSpec
import org.genivi.sota.core.data.client.ClientUpdateRequest
import org.genivi.sota.core.transfer.DefaultUpdateNotifier
import org.genivi.sota.marshalling.CirceMarshallingSupport
import java.time.Instant
import java.time.temporal.ChronoUnit

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}
import akka.http.scaladsl.unmarshalling._
import org.genivi.sota.data.Interval
import org.genivi.sota.datatype.NamespaceDirective

import scala.concurrent.Future

class UpdateRequestResourceSpec extends FunSuite
  with ScalatestRouteTest
  with ShouldMatchers
  with UpdateResourcesDatabaseSpec
  with ScalaFutures
  with DatabaseSpec
  with DefaultPatience {

  import CirceMarshallingSupport._
  import UpdateSpec._
  import NamespaceDirective._

  implicit val log = Logging(system, "UpdateRequestResourceSpec")

  val resolver = new FakeExternalResolver()
  val deviceRegistry = new FakeDeviceRegistry()

  implicit val rviClient = new ConnectivityClient {
    override def sendMessage[A](service: String, message: A, expirationDate: Instant)(implicit encoder: Encoder[A]): Future[Int] = ???
  }

  implicit val connectivity = DefaultConnectivity

  val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistry)
  val serve = new UpdateRequestsResource(db, resolver, updateService, defaultNamespaceExtractor)

  test("accepts new updates with a Client specific format") {
    val now = Instant.now
    val f = createUpdateSpec()

    whenReady(f) { case (packageModel, _, _) =>
      val req = ClientUpdateRequest(
        UUID.randomUUID(),
        packageModel.id,
        now,
        Interval(now, now.plus(1, ChronoUnit.DAYS)),
        10,
        "none",
        None,
        requestConfirmation = false
      )

      val uri = Uri.Empty.withPath(Path(s"/update_requests"))

      Post(uri, req) ~> serve.route ~> check {
        status shouldBe StatusCodes.Created
        val req = responseAs[ClientUpdateRequest]
        req.packageId shouldBe packageModel.id
      }
    }
  }
}
