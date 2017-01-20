package org.genivi.sota.resolver

import java.time.Instant

import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.{PackageId, Uuid}
import org.genivi.sota.resolver.db.ForeignPackages.InstalledForeignPackage
import org.genivi.sota.resolver.db.{DeviceRepository, ForeignPackages}
import org.slf4j.LoggerFactory
import slick.driver.MySQLDriver.api._

import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

object DeviceDataMigrator {

  private lazy val logger = LoggerFactory.getLogger(this.getClass)

  def apply(deviceRegistry: DeviceRegistry)(implicit db: Database, ec: ExecutionContext): Unit = {
    val f = for {
      rows          <- db.run(packages())
      pkgsPerDevice = mutable.Map[Uuid, Set[PackageId]]()
      _             = rows.foreach { elem =>
                        pkgsPerDevice(elem.device) = pkgsPerDevice.getOrElse(elem.device, Set()) + elem.packageId
                      }
      _             <- deviceRegistry.setInstalledPackagesForDevices(pkgsPerDevice.toSeq)
      _ = logger.info(s"Migration successful for ${rows.size} rows")
    } yield ()
    Await.result(f, 3.minutes)
  }

  def packages()(implicit db: Database, ec: ExecutionContext): DBIO[Seq[InstalledForeignPackage]] =
    for {
      nonForeignPkgRows <- DeviceRepository.listInstalledPackages
      now               = Instant.now()
      nonForeignPkgs    = nonForeignPkgRows.map{ r => InstalledForeignPackage(r._2, r._3, now) }
      foreignPkgs       <- ForeignPackages.listPackages
    } yield foreignPkgs ++ nonForeignPkgs
}
