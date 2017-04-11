package org.genivi.sota.core.resolver

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import io.circe.jawn._
import org.genivi.sota.data.{PackageId, Uuid}
import cats.syntax.show._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}

import scala.concurrent.ExecutionContext



/**
 * Spec tests for the external resolver client
 */
class ExternalResolverClientSpec extends PropSpec with Matchers with BeforeAndAfterAll {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  implicit val executionCtx = ExecutionContext.Implicits.global
  val client = new DefaultExternalResolverClient( Uri.Empty, Uri.Empty, Uri.Empty, Uri.Empty )

  val id0 = Uuid(Refined.unsafeApply(UUID.randomUUID().toString))

  val s: String = s"""{"${id0.show}": [{"version":"23.5.2","name":"rust"}]}"""

  val m: Map[Uuid, Set[PackageId]] =
    Map(id0 -> Set(PackageId(Refined.unsafeApply("rust"), Refined.unsafeApply("23.5.2"))))

  property("parse the external resolver's response") {
    decode[Map[Uuid, Set[PackageId]]](s) shouldBe Right(m)
  }

  val resp: HttpResponse = HttpResponse(entity = HttpEntity(`application/json`, s))

  property("parse from a HttpResponse as a Map") {
    ScalaFutures.whenReady(Unmarshal(resp.entity).to[Map[Uuid, Set[PackageId]]]) { m2 =>
      m2 shouldBe m
    }

  }

  override def afterAll(): Unit = {
    system.terminate()
  }

}
