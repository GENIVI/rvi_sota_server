/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core.rvi

import org.genivi.sota.core.data.Package

import scala.util.Random
import spray.json.DefaultJsonProtocol
import spray.json.JsonFormat
import spray.json.RootJsonFormat

object Protocol {
  sealed trait Action
  case class Notify(retry: Int, packageName: String) extends Action
  object Notify {
    import spray.json._
    import DefaultJsonProtocol._

    implicit object actionNotifyFormat extends RootJsonFormat[Notify] {
      def write(p: Notify): JsObject =
        JsObject(
          "retry" -> JsNumber(p.retry),
          "package" -> JsString(p.packageName)
        )

      def read(value: JsValue): Notify = value.asJsObject.getFields("retry", "package") match {
        case Seq(JsNumber(retry), JsString(packageName)) => Notify(retry.toInt, packageName)
        case _ => deserializationError("Notify param expected")
      }
    }
  }

  case class Start(foo: String) extends Action
  object Start {
    import DefaultJsonProtocol._
    implicit val format = jsonFormat1(Start.apply)
  }

  case class Chunk(foo: String) extends Action
  object Chunk {
    import DefaultJsonProtocol._
    implicit val format = jsonFormat1(Chunk.apply)
  }

  case class Download(name: String, version: String, downloadId: String) extends Action
  object Download {
    import DefaultJsonProtocol._
    implicit val format = jsonFormat3(Download.apply)
  }

  case class JsonRpcParams[A <: Action](
    service_name: String,
    timeout: Long,
    parameters: Seq[A]
  )
  object JsonRpcParams {
    import DefaultJsonProtocol._
    implicit def format[A <: Action](implicit jsonFormat: RootJsonFormat[A]): JsonFormat[JsonRpcParams[A]] =
      jsonFormat3(JsonRpcParams.apply[A])
  }

  case class JsonRpcRequest[A <: Action] (
    jsonrpc: String,
    params: JsonRpcParams[A],
    id: String,
    method: String
  )
  object JsonRpcRequest {
    import DefaultJsonProtocol._
    implicit def format[A <: Action](implicit jsonFormat: RootJsonFormat[A]): JsonFormat[JsonRpcRequest[A]] =
      jsonFormat4(JsonRpcRequest.apply[A])

    val IdLength = 8
    val Timeout = 0
    val Retry = 5

    def actionName(a: Action): String = a match {
      case Notify(_, _) => "notify"
      case Start(_) => "start"
      case Chunk(_) => "chunk"
      case Download(_, _, _) => "download"
    }

    import org.genivi.sota.core.data.Vehicle

    def build[A <: Action](vin: Vehicle.IdentificationNumber, action: A): JsonRpcRequest[A] =
      JsonRpcRequest(
        "2.0",
        JsonRpcParams(
          s"genivi.org/vin/$vin/sota/${actionName(action)}",
          Timeout,
          Seq(action)
        ),
        Random.alphanumeric.take(IdLength).mkString,
        "message"
      )

    def notifyPackage(vin: Vehicle.IdentificationNumber, pack: Package): JsonRpcRequest[Notify] =
      JsonRpcRequest.build(vin, Notify(Retry, pack.fullName))
  }
}
