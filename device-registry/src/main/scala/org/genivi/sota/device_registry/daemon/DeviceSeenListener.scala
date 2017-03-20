package org.genivi.sota.device_registry.daemon

import akka.Done
import org.genivi.sota.device_registry.db.DeviceRepository
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.{DeviceActivated, DeviceSeen}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import slick.driver.MySQLDriver.api._

object DeviceSeenListener {

  val _logger = LoggerFactory.getLogger(this.getClass)

  def action(messageBus: MessageBusPublisher)(msg: DeviceSeen)
            (implicit db: Database, ec: ExecutionContext): Future[Done] = {

          db.run(DeviceRepository.updateLastSeen(msg.uuid, msg.lastSeen)).flatMap { case (activated, ns) =>
            if(activated)
              messageBus.publishSafe(DeviceActivated(ns, msg.uuid, msg.lastSeen))
            else
              Future.successful(Done)
          }.recover {
            case ex =>
              _logger.warn(s"Could not process $msg", ex)
          }.map { _ =>
            Done
          }
    }
}
