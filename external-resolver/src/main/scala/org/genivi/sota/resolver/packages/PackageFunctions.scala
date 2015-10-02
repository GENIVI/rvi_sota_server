/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.packages

import org.genivi.sota.resolver.Errors
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcBackend.Database


object PackageFunctions {

  def exists
    (pkgId: Package.Id)
    (implicit db: Database, ec: ExecutionContext): Future[Package] =
    db.run(PackageDAO.exists(pkgId))
      .flatMap(_
        .fold[Future[Package]]
          (Future.failed(Errors.MissingPackageException))(Future.successful(_)))

}
