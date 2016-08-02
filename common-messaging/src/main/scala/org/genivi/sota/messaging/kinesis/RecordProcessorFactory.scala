package org.genivi.sota.messaging.kinesis

import akka.event.EventStream
import cats.data.Xor
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import io.circe.Decoder
import org.genivi.sota.messaging.Messages.Message

/**
  * This class exists only because the Kinesis API requires such a Factory.
  * @param eventStream A reference to the local event bus to publish to
  */
class RecordProcessorFactory[T <: Message](eventStream: EventStream)(implicit decoder: Decoder[T])
    extends IRecordProcessorFactory {

  override def createProcessor(): IRecordProcessor =  {
    new RecordProcessor(eventStream, decoder)
  }
}
