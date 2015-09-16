package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}

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

  override def afterAll(): Unit = {
    system.shutdown()
  }

}
