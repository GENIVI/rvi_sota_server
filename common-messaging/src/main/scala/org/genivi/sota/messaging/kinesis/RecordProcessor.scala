package org.genivi.sota.messaging.kinesis

import java.util

import akka.event.EventStream
import cats.data.Xor
import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorCheckpointer}
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.amazonaws.services.kinesis.model.Record
import io.circe.jawn
import org.genivi.sota.messaging.Messages
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

class RecordProcessor(eventStream: EventStream) extends IRecordProcessor {

  private val log = LoggerFactory.getLogger(this.getClass)

  override def shutdown(checkpointer: IRecordProcessorCheckpointer, reason: ShutdownReason): Unit = {
    log.trace("Shutting down worker due to reason:" + reason.toString)
  }

  override def initialize(shardId: String): Unit = {
    log.trace("Initializing kinesis worker")
  }

  /* We don't use checkpointer as the messages handled for now can be sent twice */
  override def processRecords(records: util.List[Record], checkpointer: IRecordProcessorCheckpointer): Unit = {
    for (record <- records) yield {
      jawn.parseByteBuffer(record.getData) match {
        case Xor.Left(e)     => log.error("Received unrecognized record data from Kinesis:" + e.getMessage)
        case Xor.Right(json) => Messages.parseMsg(json.toString) match {
          case Xor.Right(m) => eventStream.publish(m)
          case Xor.Left(ex) => log.error(s"Received unrecognized json from Kinesis: ${json.toString()}\n" +
            s"Got this parse error: ${ex.toString}")
        }
      }
    }
  }
}
