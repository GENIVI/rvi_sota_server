package org.genivi.sota.device_registry.daemon

import java.time.Instant

import org.genivi.sota.device_registry.db.DeviceRepository
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.messaging.Messages.{DeviceUpdateStatus, UpdateSpec}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object DeviceUpdateStatusListener {

  def action(messageBus: MessageBusPublisher)
            (implicit db: Database, system: ExecutionContext): UpdateSpec => Future[Unit] = {
    import org.genivi.sota.data.DeviceSearchCommon._
    (msg: UpdateSpec) =>
      val f = for {
        device <- DeviceRepository.findByUuid(msg.device)
        status = currentDeviceStatus(device.lastSeen, Seq((Instant.now(), msg.status)))
        _      <- DeviceRepository.setDeviceStatus(device.uuid, status)
      } yield (device, status)

      db.run(f).flatMap { case (device, status) =>
        messageBus.publish(DeviceUpdateStatus(device.namespace, device.uuid, status, Instant.now()))
      }
  }

}
