package org.genivi.sota.core

import akka.event.Logging
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.Encoder
import org.genivi.sota.core.data.UpdateRequest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSuite, ShouldMatchers}
import org.genivi.sota.marshalling.CirceMarshallingSupport
import io.circe.generic.auto._
import org.genivi.sota.core.resolver.{ConnectivityClient, DefaultConnectivity}
import org.genivi.sota.core.transfer.DefaultUpdateNotifier
import org.joda.time.DateTime

import scala.concurrent.Future

class UpdateRequestResourceSpec extends FunSuite
  with ScalatestRouteTest
  with ShouldMatchers
  with DatabaseSpec
  with UpdateResourcesDatabaseSpec
  with ScalaFutures {

  import CirceMarshallingSupport._

  implicit val log = Logging(system, "UpdateRequestResourceSpec")

  val resolver = new FakeExternalResolver()

  implicit val rviClient = new ConnectivityClient {
    override def sendMessage[A](service: String, message: A, expirationDate: _root_.com.github.nscala_time.time.Imports.DateTime)(implicit encoder: Encoder[A]): Future[Int] = ???
  }

  implicit val connectivity = DefaultConnectivity

  val serve = new UpdateRequestsResource(db, resolver, new UpdateService(DefaultUpdateNotifier))

  test("POST on /updates/:vin queues a package for update to a specific vehile") {
    val f = createUpdateSpec()

    whenReady(f) { case (packageModel, vehicle, updateSpec) =>
      val now = DateTime.now
      val uri = Uri.Empty.withPath(Path(s"/updates/${vehicle.vin.get}"))

      Post(uri, packageModel.id) ~> serve.route ~> check {
        status shouldBe StatusCodes.OK
        val updateRequest = responseAs[UpdateRequest]
        updateRequest.packageId should be (packageModel.id)
        updateRequest.creationTime.isAfter(now) shouldBe true
      }
    }
  }
}
