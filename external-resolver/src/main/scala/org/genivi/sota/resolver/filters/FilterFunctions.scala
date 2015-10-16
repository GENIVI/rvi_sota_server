/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcBackend.Database
import org.genivi.sota.resolver.common.Errors
import org.genivi.sota.resolver.packages.PackageFunctions


object FilterFunctions {

  def delete
    (name: Filter.Name)
    (implicit db: Database, ec: ExecutionContext): Future[Unit] =
    db.run(FilterRepository.delete(name))
      .flatMap(i => if (i == 0) Future.failed(Errors.MissingFilterException)
                    else Future.successful(()))

  def deleteFilterAndPackageFilters
    (name: Filter.Name)
    (implicit db: Database, ec: ExecutionContext): Future[Unit] =
    for {
      _ <- PackageFunctions.deletePackageFilterByFilterName(name)
      _ <- FilterFunctions.delete(name)
    } yield ()


}
