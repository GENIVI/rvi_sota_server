package org.genivi.sota.messaging.kinesis

import akka.actor.{ActorRef, ActorSystem}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import io.circe.Decoder
import org.genivi.sota.messaging.Messages.Message

/**
  * This class exists only because the Kinesis API requires such a Factory.
  * @param subscriber A reference to the actor to publish to
  */
class RecordProcessorFactory[T](subscriber: ActorRef)(implicit decoder: Decoder[T], system: ActorSystem)
    extends IRecordProcessorFactory {

  override def createProcessor(): IRecordProcessor =  {
    new RecordProcessor(subscriber)
  }
}
