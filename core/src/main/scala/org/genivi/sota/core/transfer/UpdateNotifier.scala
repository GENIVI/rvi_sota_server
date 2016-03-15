/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer

import akka.event.LoggingAdapter
import org.genivi.sota.core.data.UpdateSpec
import java.util.UUID
import org.genivi.sota.core.rvi.{RviClient, ServerServices}
import org.genivi.sota.data.Vehicle

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
    deriveFor[PackageUpdate].encoder

  implicit val decoder: Decoder[PackageUpdate] =
    deriveFor[PackageUpdate].decoder

}

case class UpdateNotification(update_available: PackageUpdate, services: ServerServices)

object UpdateNotification {

  import io.circe.generic.semiauto._
  import io.circe.{Decoder, Encoder}

  implicit val encoder: Encoder[UpdateNotification] =
    deriveFor[UpdateNotification].encoder

  implicit val decoder: Decoder[UpdateNotification] =
    deriveFor[UpdateNotification].decoder

  implicit val encoderServerServices: Encoder[ServerServices] =
    deriveFor[ServerServices].encoder

  implicit val decoderServerServices: Decoder[ServerServices] =
    deriveFor[ServerServices].decoder

}

/**
 * Send a notification to SOTA clients via RVI that there are packages that
 * can/should be updated.
 */
object UpdateNotifier {

  import io.circe.generic.auto._

  /**
   * Notify all the vehicles in an updated that an update is ready
   * @param updateSpecs A set of updates
   */
  def notify(updateSpecs: Seq[UpdateSpec], services: ServerServices)
            (implicit rviClient: RviClient, ec: ExecutionContext, log: LoggingAdapter): Iterable[Future[Int]] = {
    log.debug(s"Sending update notifications: $updateSpecs" )
    updateSpecs.map { spec => notifyVehicle(services)(spec.vin, spec) }
  }

  /**
   * Notify a single vehicle that it has updates
   * @param vin The VIN of the vehicle to notify
   * @param updates The updates that apply to the vehicle
   */
  def notifyVehicle(services: ServerServices)(vin: Vehicle.Vin, update: UpdateSpec)
                   (implicit rviClient: RviClient, ec: ExecutionContext): Future[Int] = {
    import com.github.nscala_time.time.Imports._
    import io.circe.generic.auto._

    def toPackageUpdate( spec: UpdateSpec ) = {
      val r = spec.request
      PackageUpdate(r.id, r.signature, r.description.getOrElse(""), r.requestConfirmation, spec.size)
    }

    val expirationDate: DateTime = update.request.periodOfValidity.getEnd
    rviClient.sendMessage(s"genivi.org/vin/${vin.get}/sota/notify",
                          UpdateNotification(toPackageUpdate(update), services), expirationDate)
  }

}
