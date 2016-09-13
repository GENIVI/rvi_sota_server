package org.genivi.sota.messaging.kinesis

import akka.actor.{ActorRef, ActorSystem}
import akka.event.EventStream
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import io.circe.{Decoder, jawn}
import io.circe.parser._
import org.genivi.sota.messaging.Messages.Message
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

class RecordProcessor[T](subscriber: ActorRef)(implicit val decoder: Decoder[T], system: ActorSystem)
  extends IRecordProcessor {

  private val log = LoggerFactory.getLogger(this.getClass)

  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    log.info("Shutting down worker due to reason:" + shutdownInput.getShutdownReason.toString)
  }

  override def initialize(initializationInput: InitializationInput): Unit = {
    log.info(s"Initializing kinesis worker on shard ${initializationInput.getShardId}")
  }

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    log.debug("Received record(s) from kinesis")
    for {
      record <- processRecordsInput.getRecords
      json   <- jawn.parseByteBuffer(record.getData).toOption
      msg    <- decode[T](json.toString).toOption
    } yield subscriber ! msg
    //TODO: Handle failure to parse messages properly, See PRO-903
    processRecordsInput.getCheckpointer.checkpoint()
  }
}
