/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import akka.http.scaladsl.util.FastFuture
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.filters.{Filter, FilterFunctions, FilterRepository}
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcBackend.Database
import org.genivi.sota.resolver.filters.FutureSupport
import FutureSupport._


object PackageFunctions {

  case object MissingPackageFilterException extends Throwable

  def exists
    (pkgId: Package.Id)
    (implicit db: Database, ec: ExecutionContext): Future[Package] =
    db.run(PackageRepository.exists(pkgId))

  def addPackageFilter
    (pf: PackageFilter)
    (implicit db: Database, ec: ExecutionContext): Future[PackageFilter] =
    for {
      _ <- PackageFunctions.exists(Package.Id(pf.packageName, pf.packageVersion))
      _ <- db.run(FilterRepository.load(pf.filterName)).failIfNone( Errors.MissingFilterException )
      _ <- db.run(PackageFilterRepository.add(pf))
    } yield pf

  def listPackagesForFilter
    (fname: Filter.Name)
    (implicit db: Database, ec: ExecutionContext): Future[Seq[Package]] =
    for {
      _  <- db.run(FilterRepository.load(fname)).failIfNone( Errors.MissingFilterException )
      ps <- db.run(PackageFilterRepository.listPackagesForFilter(fname))
    } yield ps

  def deletePackageFilter
    (pf: PackageFilter)
    (implicit db: Database, ec: ExecutionContext): Future[Unit] =
    db.run(PackageFilterRepository.deletePackageFilter(pf.packageName, pf.packageVersion, pf.filterName))
      .flatMap(i => if (i == 0) FastFuture.failed(MissingPackageFilterException)
                    else FastFuture.successful(()))

  def deletePackageFilterByFilterName
    (name: Filter.Name)
    (implicit db: Database, ec: ExecutionContext): Future[Int] =
    db.run(PackageFilterRepository.delete(name))
}
