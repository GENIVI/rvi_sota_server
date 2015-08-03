/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import org.genivi.sota.core.db.InstallRequests
import org.joda.time.DateTime
import slick.jdbc.JdbcBackend.DatabaseDef
import scala.concurrent.Future
import scala.util.Random
import spray.json.DefaultJsonProtocol

object RviRequest {
  case class RviParam(
    retry: Int,
    packageName: String
  )

  case class JsonRpcParams(
    service_name: String,
    timeout: Long,
    parameters: Seq[RviParam]
  )

  case class JsonRpc(
    jsonrpc: String,
    params: JsonRpcParams,
    id: String,
    method: String
  )

  def jsonRpcPayload(vin: String, pack: Package): JsonRpc =
    JsonRpc(
      "2.0",
      JsonRpcParams(
        s"genivi.org/vin/$vin/sota/notify",
        0,
        List(
          RviParam(
            5,
            pack.toString
          )
        )
      ),
      Random.alphanumeric.take(8).mkString,
      "message"
    )

  trait Serialization extends DefaultJsonProtocol {
    import spray.json._

    implicit object rviParamFormat extends RootJsonFormat[RviParam] {
      def write(p: RviParam) =
        JsObject(
          "retry" -> JsNumber(p.retry),
          "package" -> JsString(p.packageName)
        )

      def read(value: JsValue) = value.asJsObject.getFields("retry", "package") match {
        case Seq(JsNumber(retry), JsString(packageName)) => new RviParam(retry.toInt, packageName)
        case _ => deserializationError("RviParam expected")
      }
    }

    implicit val jsonRpcParamsFormat = jsonFormat3(JsonRpcParams.apply)
    implicit val jsonRpcFormat = jsonFormat4(JsonRpc.apply)
  }
}
import slick.driver.MySQLDriver.api.Database
class RviActor(host: String, port: Int, db : Database )
              (implicit mat: ActorMaterializer)
    extends Actor
    with ActorLogging
    with RviRequest.Serialization {

  import context._

  import HttpMethods._
  import HttpProtocols._
  import ContentTypes._

  val uri: Uri =
    Uri().
      withScheme("http").
      withAuthority(host, port).
      withPath(Uri.Path("/"))

  def receive = {
    case RviActor.Trigger => runCurrentCampaigns()
  }

  private def runCurrentCampaigns(): Future[Unit] =
    for {
      requestsWithPackages <- db.run(InstallRequests.currentAt(DateTime.now))
      _ <- Future.sequence(requestsWithPackages.map { case (req, pack) => rpc(req.vin, pack) })
      _ <- db.run( InstallRequests.updateNotified(requestsWithPackages.map(_._1)) )
    } yield ()

  private def rpc(vin: String, pack: Package): Future[HttpResponse] = {
    val payload = RviRequest.jsonRpcPayload(vin, pack)
    val serialized = jsonRpcFormat.write(payload).toString()
    Http().singleRequest(HttpRequest(POST,
                                     uri = uri,
                                     entity = HttpEntity(`application/json`, serialized)))
  }
}

object RviActor {
  val Trigger = "tick"

  def props(host: String, port: Int, db : Database)(implicit mat: ActorMaterializer): Props =
    Props(new RviActor(host, port, db))
}
