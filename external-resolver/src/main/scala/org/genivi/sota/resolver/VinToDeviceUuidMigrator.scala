/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.resolver

import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.Device.DeviceId
import org.genivi.sota.data.Namespace
import org.slf4j.LoggerFactory
import slick.driver.MySQLDriver.api._
import scala.concurrent.duration._
import org.genivi.sota.data.Device._
import cats.syntax.show.toShowOps

import scala.concurrent.{Await, ExecutionContext, Future}

object VinToDeviceUuidMigrator {
  private lazy val logger = LoggerFactory.getLogger(this.getClass)

  private val tables = List(
    "InstalledPackage",
    "Firmware",
    "InstalledComponent"
  )

  def apply(deviceRegistry: DeviceRegistry)
           (implicit db: Database, ec: ExecutionContext): Unit = {
    val vinQueries = tables.map { t =>
      sql"""SELECT namespace, device_uuid from #$t""".as[(String, String)]
    }

    val allVinsIO = DBIO.sequence(vinQueries).map(_.flatten.distinct)

    val resultF = db.run(allVinsIO) flatMap { uuids =>
      val updatesIO =
        uuids
          .map { case (ns, u) =>
            val namespace = Namespace(ns)
            val deviceId = DeviceId(u)

            deviceRegistry.fetchByDeviceId(namespace, deviceId)
              .map { d =>
                logger.info(s"Got device uuid for $u: ${d.id.show}")
                Option(d.id)
              }
              .recover {
                case ex =>
                  logger.warn(s"Could not get device uuid for ($ns, $u)", ex)
                  None
              }
              .map {
                case Some(dev) =>
                  val io = tables.map { t =>
                    sqlu"update #$t set device_uuid = ${dev.show} where device_uuid = $u"
                  }

                  DBIO.sequence(io).map(_ => Some(u))
                case None =>
                  DBIO.successful(None)
              }
          }

      Future.sequence(updatesIO) flatMap { dbIO =>
        db.run(DBIO.sequence(dbIO).transactionally)
      }
    }

    val result = Await.result(resultF, 3.minutes)

    val (success, failure) = result.partition(_.isDefined)

    logger.info(
      s"""
        |# Finished vin to UUID migration:
        |
        |Replaced device uuids for ${success.size} devices:
        |${success.flatten.mkString("\n")}
        |
        |Could not get uuids for ${failure.size} devices
      """.stripMargin
    )
  }
}
