/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.rvi

import akka.event.LoggingAdapter
import org.genivi.sota.core.data.UpdateSpec
import org.genivi.sota.core.resolver.Connectivity
import org.genivi.sota.core.transfer._
import org.genivi.sota.data.Vehicle
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}


/**
 * Send a notification to SOTA clients via RVI that there are packages that
 * can/should be updated.
 */
class RviUpdateNotifier(services: ServerServices) extends UpdateNotifier {

  import io.circe.generic.auto._

  override def notifyVehicle(vin: Vehicle.Vin, update: UpdateSpec)
                            (implicit connectivity: Connectivity, ec: ExecutionContext): Future[Int] = {
    import com.github.nscala_time.time.Imports._
    import io.circe.generic.auto._

    def toPackageUpdate( spec: UpdateSpec ) = {
      val r = spec.request
      PackageUpdate(r.id, r.signature, r.description.getOrElse(""), r.requestConfirmation, spec.size)
    }

    val expirationDate: DateTime = update.request.periodOfValidity.getEnd
    connectivity.client.sendMessage(s"genivi.org/vin/${vin.get}/sota/notify",
                                    UpdateNotification(toPackageUpdate(update), services), expirationDate)
  }

}
