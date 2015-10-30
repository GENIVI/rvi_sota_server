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

/**
 * Functions for managing packages in the resolver
 * Implements CRUD operations etc.
 */
object PackageFunctions {

  case object MissingPackageFilterException extends Throwable

  /**
   * Checks to see if a package exists
   * @param pkdId   The ID of the package
   * @return        A Future[Package] if the package exists
   * @throws        Errors.MissingPackageException if the package does not exist
   */
  def exists
    (pkgId: Package.Id)
    (implicit db: Database, ec: ExecutionContext): Future[Package] =
    db.run(PackageRepository.exists(pkgId))

  /**
   * Adds a package filter to the resolver
   * @param pf   The filter to add
   * @return     A Future[PackageFilter] for the added PackageFilter
   * @throws     Errors.MissingFilterException if the named filter does not exist
   * @throws     Errors.MissingPackageException if the named package does not exist
   */
  def addPackageFilter
    (pf: PackageFilter)
    (implicit db: Database, ec: ExecutionContext): Future[PackageFilter] =
    for {
      _ <- PackageFunctions.exists(Package.Id(pf.packageName, pf.packageVersion))
      _ <- db.run(FilterRepository.load(pf.filterName)).failIfNone( Errors.MissingFilterException )
      _ <- db.run(PackageFilterRepository.add(pf))
    } yield pf

  /**
   * Lists the packages for a filter
   * @param fname  The name of the filter for which to list the packages
   * @return       A Future[Seq[Package]] of associated packages
   * @throws       Error.MissingFilterException if the named filter does not exist
   */
  def listPackagesForFilter
    (fname: Filter.Name)
    (implicit db: Database, ec: ExecutionContext): Future[Seq[Package]] =
    for {
      _  <- db.run(FilterRepository.load(fname)).failIfNone( Errors.MissingFilterException )
      ps <- db.run(PackageFilterRepository.listPackagesForFilter(fname))
    } yield ps

  /**
   * Deletes a package filter
   * @param pf     The package filter to delete
   * @return       A Future[Unit]
   * @throws       MissingPackageFilterException if the package filter does not exist
   */
  def deletePackageFilter
    (pf: PackageFilter)
    (implicit db: Database, ec: ExecutionContext): Future[Unit] =
    db.run(PackageFilterRepository.deletePackageFilter(pf.packageName, pf.packageVersion, pf.filterName))
      .flatMap(i => if (i == 0) FastFuture.failed(MissingPackageFilterException)
                    else FastFuture.successful(()))

  /**
   * Deletes a package filter by name
   * @param name   The name of the package filter to delete
   * @return       A Future[Int] - the number of packages deleted
   * @throws       MissingPackageFilterException if the package filter does not exist
   */
  def deletePackageFilterByFilterName
    (name: Filter.Name)
    (implicit db: Database, ec: ExecutionContext): Future[Int] =
    db.run(PackageFilterRepository.delete(name))
}
