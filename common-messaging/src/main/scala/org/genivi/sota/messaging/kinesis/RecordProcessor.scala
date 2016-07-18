package org.genivi.sota.messaging.kinesis

import akka.event.EventStream
import cats.data.Xor
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import io.circe.jawn
import org.genivi.sota.messaging.Messages
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

class RecordProcessor(eventStream: EventStream) extends IRecordProcessor {

  private val log = LoggerFactory.getLogger(this.getClass)

  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    log.debug("Shutting down worker due to reason:" + shutdownInput.getShutdownReason.toString)
  }

  override def initialize(initializationInput: InitializationInput): Unit = {
    log.debug(s"Initializing kinesis worker on shard ${initializationInput.getShardId}")
  }

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    log.trace("Received record(s) from kinesis")
    for (record <- processRecordsInput.getRecords) yield {
      jawn.parseByteBuffer(record.getData) match {
        case Xor.Left(e)     => log.error("Received unrecognized record data from Kinesis:" + e.getMessage)
        case Xor.Right(json) => Messages.parseMsg(json.toString) match {
          case Xor.Right(m) => eventStream.publish(m)
          case Xor.Left(ex) => log.error(s"Received unrecognized json from Kinesis: ${json.toString()}\n" +
            s"Got this parse error: ${ex.toString}")
        }
      }
    }
    //TODO: Handle failure to parse messages properly, See PRO-903
    processRecordsInput.getCheckpointer.checkpoint()
  }
}
