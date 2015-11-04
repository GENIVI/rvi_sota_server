package org.genivi.sota.core.jsonrpc

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.circe._
import io.circe.syntax._
import org.scalatest.{PropSpec, Matchers, BeforeAndAfterAll}
import org.scalatest.prop.PropertyChecks
import scala.concurrent.Future

/**
 * Base class for JSON-RPC property-based specs
 */
abstract class JsonRpcSpecBase extends PropSpec with PropertyChecks with  Matchers with JsonGen with BeforeAndAfterAll {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  implicit val exec = system.dispatcher

  def resultJson( id: Int, result: Json ) = Json.obj(
    "jsonrpc" -> "2.0".asJson,
    "result"  -> result,
    "id"      -> id.asJson
  )

  override def afterAll() {
    system.terminate()
  }

}
