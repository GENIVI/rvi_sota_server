package org.genivi.sota.messaging.kinesis

import akka.event.EventStream
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}

/**
  * This class exists only because the Kinesis API requires such a Factory.
  * @param eventStream A reference to the local event bus to publish to
  */
class RecordProcessorFactory(eventStream: EventStream) extends IRecordProcessorFactory {

  override def createProcessor(): IRecordProcessor =  {
    new RecordProcessor(eventStream)
  }
}
