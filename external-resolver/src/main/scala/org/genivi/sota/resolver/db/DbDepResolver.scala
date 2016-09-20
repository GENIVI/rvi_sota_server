/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.db

import java.util.UUID

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import eu.timepit.refined._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.data.Device.DeviceId
import org.genivi.sota.data.{Device, Namespace, PackageId}
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.components.Component.PartNumber
import org.genivi.sota.resolver.filters.{And, Filter, FilterAST, True}
import org.genivi.sota.resolver.resolve.ResolveFunctions
import slick.backend.DatabasePublisher
import slick.driver.MySQLDriver.api._
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}

case class DeviceIdPackages(device: Device.Id, vin: Option[DeviceId],
                            packageIds: Seq[PackageId], parts: Seq[PartNumber]) {
  def filterable: (Device.DeviceId, (Seq[PackageId], Seq[PartNumber])) =
    (vin.getOrElse(DeviceId("")), (packageIds, parts))

  def +(other: DeviceIdPackages): DeviceIdPackages =
    copy(packageIds = this.packageIds ++ other.packageIds, parts = this.parts ++ other.parts)
}

object DbDepResolver {
  import Device._
  import cats.syntax.show._

  type DeviceComponentRow = (Device.Id, Option[PackageId.Name], Option[PackageId.Version], Option[Component.PartNumber])

 /*
  * Resolving package dependencies.
  */
  def resolve(namespace: Namespace, deviceRegistry: DeviceRegistry, pkgId: PackageId)
             (implicit db: Database, ec: ExecutionContext,
              mat: Materializer): Future[Map[Device.Id, Seq[PackageId]]] = {
    for {
      devices <- deviceRegistry.listNamespace(namespace)
      filtersForPkg <- db.run(PackageFilterRepository.listFiltersForPackage(namespace, pkgId))
      vf <- filterDevices(namespace, devices.map(d => (d.id, d.deviceId)).toMap, filterByPackageFilters(filtersForPkg))
    } yield ResolveFunctions.makeFakeDependencyMap(pkgId, vf)
  }

  def filterDevices(namespace: Namespace, devices: Map[Device.Id, Option[DeviceId]], filter: FilterAST)
                   (implicit db: Database, ec: ExecutionContext, mat: Materializer): Future[Seq[Device.Id]] = {
    val allDevicesPublisher = allDeviceComponents(devices.keys.toSeq, namespace)

    Source.fromPublisher(allDevicesPublisher)
      .via(toVinPackages(devices))
      .via(groupByDevice())
      .via(filterFlowFrom(filter))
      .map(_.device)
      .runFold(Vector.empty[Device.Id])(_ :+ _)
  }

  protected def allDeviceComponents(devices: Seq[Device.Id], namespace: Namespace)
                                   (implicit db: Database,
                                    ec: ExecutionContext): DatabasePublisher[DeviceComponentRow] = {

    val tmptable = s"""device_ids_tmp_${UUID.randomUUID().toString.replace("-", "")}"""

    val createTmpTableIO =
      sqlu"""
            CREATE TEMPORARY TABLE #$tmptable
           (`device_id` char(36) not null PRIMARY KEY)
          """

    val inserts =
      devices.map(d => sqlu"insert into #$tmptable values ('#${d.show}');")

    implicit val queryResParse = GetResult { r =>
      val deviceId = refineV[Device.ValidId](r.nextString).right.map(Device.Id).right.get

      val packageName = r.nextStringOption() flatMap { o =>
        refineV[PackageId.ValidName](o).right.toOption
      }

      val packageVersion = r.nextStringOption() flatMap { o =>
        refineV[PackageId.ValidVersion](o).right.toOption
      }

      val partNumber = r.nextStringOption() flatMap { o =>
        refineV[Component.ValidPartNumber](o).right.toOption
      }

      (deviceId, packageName, packageVersion, partNumber)
    }

    val queryIO =
      sql"""
            SELECT tmp.device_id, ip.packageName, ip.packageVersion, ic.partNumber
             from #$tmptable tmp
              left join #${DeviceRepository.installedPackages.baseTableRow.tableName} ip
              ON ip.device_uuid = tmp.device_id
              left join #${DeviceRepository.installedComponents.baseTableRow.tableName} ic
              ON ic.device_uuid = tmp.device_id
              order by tmp.device_id asc
        """.as[(Device.Id, Option[PackageId.Name], Option[PackageId.Version], Option[PartNumber])]

    val dbIO = createTmpTableIO
      .andThen(DBIO.sequence(inserts))
      .flatMap(_ => queryIO)
      .transactionally

    db.stream(dbIO)
  }

  /**
    * Pass on, except it wraps into [[PackageId]] a pair [[PackageId.Name]], [[PackageId.Version]]
    */
  protected def toVinPackages(deviceIdMapping: Map[Device.Id, Option[DeviceId]])
  : Flow[DeviceComponentRow, DeviceIdPackages, NotUsed] = {
    Flow[DeviceComponentRow].map {
      case (v, pName, pVersion, partNumber) =>
        val packageId = for {
          n <- pName
          v <- pVersion
        } yield PackageId(n, v)

        val vin = deviceIdMapping(v)

        DeviceIdPackages(v, vin, packageId.toSeq, partNumber.toSeq)
    }
  }

  protected def insertMissingDevices(devices: Seq[Device.Id])(implicit ec: ExecutionContext,
                                       mat: Materializer): Flow[DeviceIdPackages, DeviceIdPackages, NotUsed] = ???

  protected def groupByDevice()(implicit ec: ExecutionContext,
                                mat: Materializer): Flow[DeviceIdPackages, DeviceIdPackages, NotUsed] = {
    val groupByVin = GroupedByPredicate[DeviceIdPackages, Device.Id](_.device)

    Flow[DeviceIdPackages]
      .via(groupByVin)
      .map(l => l.tail.foldRight(l.head)(_ + _))
  }

  /**
    * Utility to parse a Seq of [[Filter]] into a single [[FilterAST]] that AND-s them.
    */
  protected def filterByPackageFilters(filters: Seq[Filter]): FilterAST = {
    filters
      .map(_.expression)
      .map(FilterAST.parseValidFilter)
      .foldLeft[FilterAST](True)(And)
  }

  /**
    * Only pass on those [[DeviceIdPackages]] that satisfy the given [[FilterAST]]
    */
  protected def filterFlowFrom(filterAST: FilterAST): Flow[DeviceIdPackages, DeviceIdPackages, NotUsed] = {
    val predicate = FilterAST.query(filterAST)

    Flow[DeviceIdPackages]
      .filter(v => predicate.apply(v.filterable))
  }
}


