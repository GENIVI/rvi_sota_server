/*
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */

package org.genivi.sota.resolver.vehicles

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import org.genivi.sota.data.Namespace.Namespace
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.resolver.components.Component
import org.genivi.sota.resolver.components.Component.PartNumber
import org.genivi.sota.resolver.filters.{And, Filter, True}
import org.genivi.sota.resolver.packages.PackageFilterRepository
import org.genivi.sota.resolver.resolve.ResolveFunctions
import slick.backend.DatabasePublisher
import slick.driver.MySQLDriver.api._
import org.genivi.sota.resolver.filters.FilterAST

import scala.concurrent.{ExecutionContext, Future}

case class VinPackages(vehicle: Vehicle, packageIds: Seq[PackageId], parts: Seq[PartNumber]) {
  def tupled = (vehicle, (packageIds, parts))

  def +(other: VinPackages): VinPackages =
    copy(packageIds = this.packageIds ++ other.packageIds, parts = this.parts ++ other.parts)
}

object DbDepResolver {
  import org.genivi.sota.refined.SlickRefined._

  type VinComponentRow = (Vehicle, Option[PackageId.Name], Option[PackageId.Version], Option[Component.PartNumber])

  /*
 * Resolving package dependencies.
 */
  def resolve(db: Database, namespace: Namespace, pkgId: PackageId)
             (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[Map[Vehicle.Vin, Seq[PackageId]]] = {
    for {
      filtersForPkg <- db.run(PackageFilterRepository.listFiltersForPackage(namespace, pkgId))
      vf <- vehiclesForFilter(namespace, db, filterByPackageFilters(filtersForPkg))
    } yield ResolveFunctions.makeFakeDependencyMap(pkgId, vf)
  }

  def vehiclesForFilter(namespace: Namespace, db: Database, filter: FilterAST)
                       (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[Seq[Vehicle]] = {
    val allVehiclesPublisher = allVehicleComponents(db, namespace)

    Source.fromPublisher(allVehiclesPublisher)
      .via(toVinPackages)
      .via(groupByVehicle())
      .via(filterFlowFrom(filter))
      .map(_.vehicle)
      .runFold(Vector.empty[Vehicle])(_ :+ _)
  }

  protected def allVehicleComponents(db: Database, namespace: Namespace)
                          (implicit ec: ExecutionContext): DatabasePublisher[VinComponentRow] = {
    val q = VehicleRepository.vehicles
      .filter(_.namespace === namespace)
      .joinLeft(VehicleRepository.installedPackages).on(_.vin === _.vin)
      .joinLeft(VehicleRepository.installedComponents).on(_._1.vin === _.vin)
      .map { case ((v, ip), ic) =>
        (v, ip.map(_.packageName), ip.map(_.packageVersion), ic.map(_.partNumber))
      }
      .sortBy(_._1.vin)
      .result

    db.stream(q)
  }

  protected def toVinPackages: Flow[VinComponentRow, VinPackages, NotUsed] = {
    Flow[VinComponentRow].map {
      case (v, pName, pVersion, partNumber) =>
        val packageId = for {
          n <- pName
          v <- pVersion
        } yield PackageId(n, v)

        VinPackages(v, packageId.toSeq, partNumber.toSeq)
    }
  }

  protected def groupByVehicle()(implicit ec: ExecutionContext,
                                 mat: ActorMaterializer): Flow[VinPackages, VinPackages, NotUsed] = {
    val groupByVin = GroupedByPredicate[VinPackages, Vehicle.Vin](_.vehicle.vin)

    Flow[VinPackages]
      .via(groupByVin)
      .map(l => l.tail.foldRight(l.head)(_ + _))
  }

  protected def filterByPackageFilters(filters: Seq[Filter]): FilterAST = {
    filters
      .map(_.expression)
      .map(FilterAST.parseValidFilter)
      .foldLeft[FilterAST](True)(And)
  }

  protected def filterFlowFrom(filterAST: FilterAST): Flow[VinPackages, VinPackages, NotUsed] = {
    Flow[VinPackages]
      .filter(v => FilterAST.query(filterAST).apply(v.tupled))
  }
}


