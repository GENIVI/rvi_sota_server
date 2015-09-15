/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.transfer

import cats.Foldable
import io.circe.Json
import org.genivi.sota.core.data.{Vehicle, PackageId, UpdateSpec}
import org.genivi.sota.core.rvi.Protocol
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import org.genivi.sota.core.rvi.ServerServices

import scala.concurrent.Future

case class PackageUpdate( `package`: PackageId, size: Long )
case class UpdateNotification(packages: Seq[PackageUpdate], services: ServerServices)

object UpdateNotifier {

  def notify(updateSpecs: Seq[UpdateSpec], services: ServerServices)
            (implicit transport: Json => Future[Json], ec: ExecutionContext): Iterable[Future[Int]] = {
    val updatesByVin : Map[Vehicle.IdentificationNumber, Seq[UpdateSpec]] = updateSpecs.groupBy( _.vin )
    updatesByVin.map( (notifyVehicle(services) _ ).tupled  )
  }

  def notifyVehicle(services: ServerServices)( vin: Vehicle.IdentificationNumber, updates: Seq[UpdateSpec] )
                   ( implicit transport: Json => Future[Json], ec: ExecutionContext): Future[Int] = {
    import com.github.nscala_time.time.Imports._
    import io.circe.generic.auto._

    def toPackageUpdate( spec: UpdateSpec ) = {
      val packageId = spec.request.packageId
      PackageUpdate( packageId, spec.size)
    }

    val earliestExpirationDate : DateTime = updates.map( _.request.periodOfValidity.getEnd ).min
    Protocol.sendMessage( s"genivi.org/vin/${vin.get}/sota/notify", UpdateNotification(updates.map(toPackageUpdate), services), earliestExpirationDate )
  }

}
