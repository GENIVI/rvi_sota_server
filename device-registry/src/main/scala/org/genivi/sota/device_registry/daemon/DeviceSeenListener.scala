package org.genivi.sota.device_registry.daemon

import akka.Done
import org.genivi.sota.device_registry.db.DeviceRepository
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.{DeviceActivated, DeviceSeen}

import scala.concurrent.{ExecutionContext, Future}
import scala.async.Async._
import slick.driver.MySQLDriver.api._

object DeviceSeenListener {

  def action(messageBus: MessageBusPublisher)(msg: DeviceSeen)
            (implicit db: Database, ec: ExecutionContext): Future[Done] =
    async {
      val (activated, ns) = await(db.run(DeviceRepository.updateLastSeen(msg.uuid, msg.lastSeen)))
      if (activated) {
        await(messageBus.publish(DeviceActivated(ns, msg.uuid, msg.lastSeen)).map(_ => Done))
      } else {
        Done
      }
    }
}
