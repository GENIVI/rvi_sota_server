package org.genivi.sota.messaging

import akka.event.EventStream
import org.genivi.sota.messaging.Messages.DeviceSeenMessage

trait MessageBusClient {

  //This is called a 'subject' in NATS and 'parition key' in Kinesis
  //Since the MessageBroker receives all messages, there is no need to perform
  //sharding of messages, so we use the same key for all messages for now
  val topic = "ota-plus"

  /**
    * This method is used to subscribe the messaging platform to the akka event stream
    * @param eventStream a reference to the Akka event stream in use by the web server
    */
  def init(eventStream: EventStream): Unit

  def sendMsg(msg: DeviceSeenMessage): Unit

  def shutdown(): Unit
}
