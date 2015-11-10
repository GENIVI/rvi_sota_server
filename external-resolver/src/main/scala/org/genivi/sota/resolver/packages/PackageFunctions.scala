/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import akka.http.scaladsl.util.FastFuture
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.filters.{Filter, FilterFunctions, FilterRepository}
import scala.concurrent.{ExecutionContext, Future}
import slick.driver.MySQLDriver.api._
import slick.jdbc.JdbcBackend.Database

/**
 * Functions for managing packages in the resolver
 * Implements CRUD operations etc.
 */
object PackageFunctions {

  case object MissingPackageFilterException extends Throwable

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
