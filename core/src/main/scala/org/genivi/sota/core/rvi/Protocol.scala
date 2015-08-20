/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core.rvi

import akka.util.ByteString
import sun.misc.BASE64Encoder

import org.genivi.sota.core.data.Package
import spray.json.{DefaultJsonProtocol, JsonFormat, RootJsonFormat}

import scala.util.Random

object Protocol {
  sealed trait Action
  case class Notify(retry: Int, packageName: String) extends Action
  object Notify extends DefaultJsonProtocol {
    import spray.json._

    implicit object notifyFormat extends JsonFormat[Notify] {
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

  case class TransferStart(totalSize: Long, packageIdentifier: String, chunkSize: Long, checksum: String) extends Action
  object TransferStart extends DefaultJsonProtocol {
    import spray.json._

    implicit object transferStartFormat extends JsonFormat[TransferStart] {
      def write(p: TransferStart): JsObject =
        JsObject(
          "total_size" -> JsNumber(p.totalSize),
          "package" -> JsString(p.packageIdentifier),
          "chunk_size" -> JsNumber(p.chunkSize),
          "checksum" -> JsString(p.checksum)
        )

      def read(value: JsValue): TransferStart = value.asJsObject.getFields("total_size", "package", "chunk_size", "checksum") match {
        case Seq(JsNumber(totalSize), JsString(packageIdentifier), JsNumber(chunkSize), JsString(checksum)) =>
          TransferStart(totalSize.toLong, packageIdentifier, chunkSize.toLong, checksum)
        case _ => deserializationError("TransferStart message expected")
      }
    }
  }

  case class TransferChunk(index: Long, msg: String) extends Action
  object TransferChunk extends DefaultJsonProtocol {
    implicit val transferChunkFormat = jsonFormat2(TransferChunk.apply)
  }

  case class TransferFinish(id: Long) extends Action
  object TransferFinish extends DefaultJsonProtocol {
    implicit val transferFinishFormat = jsonFormat1(TransferFinish.apply)
  }

  case class Download(packageIdentifier: String, destination: String) extends Action
  object Download extends DefaultJsonProtocol {
    implicit val downloadFormat = jsonFormat2(Download.apply)
  }

  case class JsonRpcParams[A <: Action](
    service_name: String,
    timeout: Long,
    parameters: Seq[A]
  )
  object JsonRpcParams extends DefaultJsonProtocol {
    implicit def jsonRpcParamsFormat[A <: Action :JsonFormat]: JsonFormat[JsonRpcParams[A]] =
      jsonFormat3(JsonRpcParams.apply[A])
  }

  case class JsonRpcRequest[A <: Action] (
    jsonrpc: String,
    params: JsonRpcParams[A],
    id: Long,
    method: String
  )
  object JsonRpcRequest extends DefaultJsonProtocol {
    implicit def format[A <: Action :JsonFormat]: RootJsonFormat[JsonRpcRequest[A]] =
      jsonFormat4(JsonRpcRequest.apply[A])

    val IdLength = 8
    val Timeout = 0
    val Retry = 5

    def actionName(a: Action): String = a match {
      case Notify(_, _) => "notify"
      case TransferStart(_, _, _, _) => "start"
      case TransferChunk(_, _) => "chunk"
      case TransferFinish(_) => "finish"
      case Download(_, _) => "download"
    }

    import org.genivi.sota.core.data.Vehicle

    def build[A <: Action](destination: String, action: A, id: Long = Random.nextLong()): JsonRpcRequest[A] =
      JsonRpcRequest(
        "2.0",
        JsonRpcParams(
          s"genivi.org$destination/${actionName(action)}",
          Timeout,
          Seq(action)
        ),
        id,
        "message"
      )

    private val base64Encoder: BASE64Encoder = new BASE64Encoder()

    def notifyPackage(vin: Vehicle.IdentificationNumber, pack: Package): JsonRpcRequest[Notify] =
      JsonRpcRequest.build(s"/vin/$vin/sota", Notify(Retry, s"${pack.id.name}-${pack.id.version}"))

    def transferStart(transactionId: Long, destination: String, packageIdentifier: String, totalSize: Long, chunkSize: Long,
                      checksum: String): JsonRpcRequest[TransferStart] =
      JsonRpcRequest.build(destination, TransferStart(totalSize, packageIdentifier, chunkSize, checksum), transactionId)

    def transferChunk(transactionId: Long, destination: String, index: Long, data: ByteString): JsonRpcRequest[TransferChunk] =
      JsonRpcRequest.build(destination, TransferChunk(index, base64Encoder.encode(data.asByteBuffer)), transactionId)

    def transferFinish(transactionId: Long, destination: String): JsonRpcRequest[TransferFinish] =
      JsonRpcRequest.build(destination, TransferFinish(transactionId), transactionId)
  }
}
