/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.core.rvi

import org.genivi.sota.core.data.{InstallRequest, Package}
import org.genivi.sota.core.db.InstallRequests
import org.genivi.sota.core.files.Types
import org.joda.time.DateTime
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object DeviceCommunication {
  type ErrorLogger = (Throwable => Unit)
}

class DeviceCommunication(db : Database,
                          rviNode: RviInterface,
                          resolveFile: Types.Resolver,
                          logError: DeviceCommunication.ErrorLogger)
                         (implicit ec: ExecutionContext) {

  private def notify(payload: (InstallRequest, Package)): Future[Try[InstallRequest]] = payload match {
    case (req, pack) => rviNode.notify(req.vin, pack)
        .map(_ => Success(req))
        .recover { case e@_ => Failure(e) }
  }

  def runCurrentCampaigns(): Future[Unit] = for {
    reqsWithPackages <- db.run(InstallRequests.currentAt(DateTime.now))
    allResponses <- Future.sequence(reqsWithPackages.map(notify _))
    successful = allResponses.collect { case Success(x) => x }
    failed = allResponses.collect { case Failure(e) => logError(e) }
    _ <- db.run(InstallRequests.updateNotified(successful))
  } yield ()
}
