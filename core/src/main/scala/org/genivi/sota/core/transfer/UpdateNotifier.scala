/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer

import akka.event.LoggingAdapter
import io.circe.Json
import org.genivi.sota.core.data.{Vehicle, Package, UpdateSpec}
import org.genivi.sota.core.rvi.{RviClient,ServerServices}
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

import scala.concurrent.Future

case class PackageUpdate( `package`: Package.Id, size: Long )

object PackageUpdate {

  import io.circe.{Encoder, Decoder}
  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[PackageUpdate] =
    deriveFor[PackageUpdate].encoder

  implicit val decoder: Decoder[PackageUpdate] =
    deriveFor[PackageUpdate].decoder

}

case class UpdateNotification(packages: Seq[PackageUpdate], services: ServerServices)

object UpdateNotification {

  import io.circe.{Encoder, Decoder}
  import io.circe.generic.semiauto._

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
    val updatesByVin : Map[Vehicle.Vin, Seq[UpdateSpec]] = updateSpecs.groupBy( _.vin )
    updatesByVin.map( (notifyVehicle(services) _ ).tupled  )
  }

  /**
   * Notify a single vehicle that it has updates
   * @param vin The VIN of the vehicle to notify
   * @param updates The updates that apply to the vehicle
   */
  def notifyVehicle(services: ServerServices)( vin: Vehicle.Vin, updates: Seq[UpdateSpec] )
                   ( implicit rviClient: RviClient, ec: ExecutionContext): Future[Int] = {
    import com.github.nscala_time.time.Imports._
    import io.circe.generic.auto._

    def toPackageUpdate( spec: UpdateSpec ) = {
      val packageId = spec.request.packageId
      PackageUpdate( packageId, spec.size)
    }

    val earliestExpirationDate : DateTime = updates.map( _.request.periodOfValidity.getEnd ).min
    rviClient.sendMessage( s"genivi.org/vin/${vin.get}/sota/notify",
                           UpdateNotification(updates.map(toPackageUpdate), services), earliestExpirationDate )
  }

}
