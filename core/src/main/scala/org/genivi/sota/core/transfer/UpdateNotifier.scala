/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer

import akka.event.LoggingAdapter
import java.util.UUID

import org.genivi.sota.core.data.UpdateSpec
import org.genivi.sota.core.data.{Package, UpdateSpec}
import org.genivi.sota.core.resolver.Connectivity
import org.genivi.sota.core.rvi.ServerServices
import org.genivi.sota.data.Vehicle
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}


case class PackageUpdate(update_id: UUID,
                         signature: String,
                         description: String,
                         request_confirmation: Boolean,
                         size: Long)

object PackageUpdate {

  import io.circe.generic.semiauto._
  import io.circe.{Decoder, Encoder}

  implicit val encoder: Encoder[PackageUpdate] =
    deriveEncoder[PackageUpdate]

  implicit val decoder: Decoder[PackageUpdate] =
    deriveDecoder[PackageUpdate]

}

case class UpdateNotification(update_available: PackageUpdate, services: ServerServices)

object UpdateNotification {

  import io.circe.generic.semiauto._
  import io.circe.{Decoder, Encoder}

  implicit val encoder: Encoder[UpdateNotification] =
    deriveEncoder[UpdateNotification]

  implicit val decoder: Decoder[UpdateNotification] =
    deriveDecoder[UpdateNotification]

  implicit val encoderServerServices: Encoder[ServerServices] =
    deriveEncoder[ServerServices]

  implicit val decoderServerServices: Decoder[ServerServices] =
    deriveDecoder[ServerServices]

}

trait UpdateNotifier {

  /**
   * Notify all the vehicles that an update is ready
   * @param updateSpecs A set of updates
   */
  def notify(updateSpecs: Seq[UpdateSpec])
            (implicit connectivity: Connectivity,
             ec: ExecutionContext,
             log: LoggingAdapter): Iterable[Future[Int]] = {
    log.debug(s"Sending update notifications: $updateSpecs" )
    updateSpecs.map { spec => notifyVehicle(spec.vin, spec) }
  }

  /**
   * Notify a single vehicle that it has updates
   * @param vin The VIN of the vehicle to notify
   * @param updates The updates that apply to the vehicle
   */
  def notifyVehicle(vin: Vehicle.Vin, update: UpdateSpec)
                   (implicit connectivity: Connectivity, ec: ExecutionContext): Future[Int]
}

object DefaultUpdateNotifier extends UpdateNotifier {

  override def notifyVehicle(vin: Vehicle.Vin, update: UpdateSpec)
                            (implicit connectivity: Connectivity, ec: ExecutionContext) = {
    // TODO: missing default implementation
    Future.successful(0)
  }
}
