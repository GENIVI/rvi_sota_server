package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import cats.data.Xor
import eu.timepit.refined.Refined
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}
import org.genivi.sota.core.data.{Vehicle, Package}

import scala.concurrent.Future

/**
 * Created by vladimir on 20/08/15.
 */
class ExternalResolverClientSpec extends PropSpec with Matchers with BeforeAndAfterAll {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  val client = new DefaultExternalResolverClient( Uri.Empty, Uri.Empty, Uri.Empty )

  property("handles failed put requests") {
    val error = new Throwable("ups")
    ScalaFutures.whenReady( client.handlePutResponse( Future.failed( error ) ).failed ) { e =>
      e shouldBe ExternalResolverRequestFailed( error )
    }
  }

  property("handles unexpected status codes") {
    ScalaFutures.whenReady( client.handlePutResponse( Future.successful( HttpResponse(StatusCodes.BadRequest) ) ).failed ) { e =>
      e shouldBe a [ExternalResolverRequestFailed]
    }
  }

  val s: String = s"""[[{"get":"VINBEAGLEBOARD000"},[{"version":{"get":"23.5.2"},"name":{"get":"rust"}}]]]"""

  val m: Map[Vehicle.IdentificationNumber, Set[Package.Id]] =
    Map(Refined("VINBEAGLEBOARD000") -> Set(Package.Id(Refined("rust"), Refined("23.5.2"))))

  property("parse the external resolver's response") {

    import io.circe.generic.auto._
    import io.circe.jawn._

    decode[Map[Vehicle.IdentificationNumber, Set[Package.Id]]](s) shouldBe Xor.Right(m)
  }

  val s2: String = s"""[[{"get":"VINBEAGLEBOARD000"},[{"version":"23.5.2","name":"rust"}]]]"""

  override def afterAll(): Unit = {
    system.shutdown()
  }

}
