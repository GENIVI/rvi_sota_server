package org.genivi.sota.core

import java.util.UUID

import akka.event.Logging
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.Encoder
import io.circe.generic.auto._
import org.genivi.sota.core.resolver.{ConnectivityClient, DefaultConnectivity}
import org.genivi.sota.core.data.client.ClientUpdateRequest
import org.genivi.sota.core.transfer.DefaultUpdateNotifier
import org.genivi.sota.marshalling.CirceMarshallingSupport
import java.time.Instant
import java.time.temporal.ChronoUnit

import org.genivi.sota.DefaultPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}
import org.genivi.sota.data.{Interval, Namespaces}
import org.genivi.sota.http.NamespaceDirectives
import org.genivi.sota.messaging.MessageBusPublisher

import scala.concurrent.Future

class UpdateRequestResourceSpec extends FunSuite
  with ScalatestRouteTest
  with ShouldMatchers
  with UpdateResourcesDatabaseSpec
  with ScalaFutures
  with DatabaseSpec
  with DefaultPatience
  with LongRequestTimeout {

  import CirceMarshallingSupport._
  import NamespaceDirectives._

  implicit val log = Logging(system, "UpdateRequestResourceSpec")

  val resolver = new FakeExternalResolver()
  val deviceRegistry = new FakeDeviceRegistry(Namespaces.defaultNs)

  implicit val rviClient = new ConnectivityClient {
    override def sendMessage[A](service: String, message: A, expirationDate: Instant)
                               (implicit encoder: Encoder[A]): Future[Int] = ???
  }

  implicit val connectivity = DefaultConnectivity

  val updateService = new UpdateService(DefaultUpdateNotifier, deviceRegistry)
  val messageBus = MessageBusPublisher.ignore
  val serve = new UpdateRequestsResource(db, resolver, updateService, defaultNamespaceExtractor, messageBus)

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
