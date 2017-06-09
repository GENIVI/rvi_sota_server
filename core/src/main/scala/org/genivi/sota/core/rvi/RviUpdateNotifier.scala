/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.rvi

import org.genivi.sota.core.data.UpdateSpec
import org.genivi.sota.core.resolver.Connectivity
import org.genivi.sota.core.transfer._
import java.time.Instant
import org.genivi.sota.data.Uuid
import scala.concurrent.{ExecutionContext, Future}


/**
 * Send a notification to SOTA clients via RVI that there are packages that
 * can/should be updated.
 */
class RviUpdateNotifier(services: ServerServices) extends UpdateNotifier {

  override def notifyDevice(device: Uuid, update: UpdateSpec)
                            (implicit connectivity: Connectivity, ec: ExecutionContext): Future[Int] = {
    import io.circe.generic.auto._

    def toPackageUpdate( spec: UpdateSpec ) = {
      val r = spec.request
      PackageUpdate(r.id, r.signature, r.description.getOrElse(""), r.requestConfirmation, spec.size)
    }

    val expirationDate: Instant = update.request.periodOfValidity.end
    connectivity.client.sendMessage(s"genivi.org/device/${device.underlying.value}/sota/notify",
                                    UpdateNotification(toPackageUpdate(update), services), expirationDate)
  }

}
