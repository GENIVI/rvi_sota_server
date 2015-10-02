/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.filters

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import eu.timepit.refined.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.db.PackageFilters
import org.genivi.sota.resolver.types.PackageFilter
import org.genivi.sota.resolver.filters._
import org.genivi.sota.resolver.packages._
import org.genivi.sota.resolver.vehicles._
import org.genivi.sota.rest.Validation._
import org.genivi.sota.resolver.common.Errors
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.jdbc.JdbcBackend.Database


object FutureSupport {

  implicit class FutureOps[T]( x: Future[Option[T]] ) {

    def failIfNone( t: Throwable )
                  (implicit ec: ExecutionContext): Future[T] =
      x.flatMap( _.fold[Future[T]]( FastFuture.failed(t) )(FastFuture.successful) )

  }

}

class FilterDirectives()(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext) {
  import FutureSupport._
  import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
  import org.genivi.sota.marshalling.RefinedMarshallingSupport._

  def filterRoute: Route =
    pathPrefix("filters") {
      get {
        parameter('regex.as[Refined[String, Regex]].?) { re =>
          val query = re.fold(FilterRepository.list)(r => FilterRepository.searchByRegex(r.get))
          complete(db.run(query))
        }
      } ~
      (post & entity(as[Filter])) { filter =>
        complete(db.run(FilterRepository.add(filter)))
      } ~
      (put & refined[Filter.ValidName](Slash ~ Segment ~ PathEnd)
           & entity(as[Filter.ExpressionWrapper])) { (fname, expr) =>
          complete(db.run(FilterRepository.update(Filter(fname, expr.expression))).failIfNone( Errors.MissingFilterException ))
      } ~
      (delete & refined[Filter.ValidName](Slash ~ Segment ~ PathEnd)) { fname =>
        complete( db.run(FilterRepository.delete(fname)).flatMap(x => if(x == 0) FastFuture.failed( Errors.MissingFilterException ) else FastFuture.successful(()) ) )
      }

    }

  def add(pf: PackageFilter) : Future[PackageFilter] = {
    for {
      _ <- PackageFunctions.exists(Package.Id(pf.packageName, pf.packageVersion))
      _ <- db.run(FilterRepository.load(pf.filterName)).failIfNone( Errors.MissingFilterException )
      _ <- db.run(PackageFilters.add(pf))
    } yield pf
  }

  def listPackagesForFilter(fname: Filter.Name): Future[Seq[Package]] =
    for {
      _  <- db.run(FilterRepository.load(fname)).failIfNone( Errors.MissingFilterException )
      ps <- db.run(PackageFilters.listPackagesForFilter(fname))
    } yield ps

  def packageFiltersRoute: Route =

    pathPrefix("packageFilters") {
      get {
        parameters('package.as[Package.NameVersion].?, 'filter.as[Filter.Name].?) {
          case (Some(nameVersion), None) =>
            val packageName: Package.Name = Refined(nameVersion.get.split("-").head)
            val packageVersion: Package.Version = Refined(nameVersion.get.split("-").tail.head)
            val f: Future[Seq[Filter]] = for {
              (p, fs) <- db.run(PackageFilters.listFiltersForPackage(Package.Id(packageName, packageVersion)))
              _       <- p.fold[Future[Package]]( FastFuture.failed( Errors.MissingPackageException ) )( FastFuture.successful )
            } yield fs
            complete(f)

          case (None, Some(fname)) =>
            complete(listPackagesForFilter(fname) )
          case (None, None) =>
            complete(db.run(PackageFilters.list))
          case _ =>
            complete(StatusCodes.NotFound)
        }
      } ~
      (post & entity(as[PackageFilter])) { pf =>
        complete(add(pf))
      } ~
      (delete & refined[Package.ValidName]   (Slash ~ Segment)
              & refined[Package.ValidVersion](Slash ~ Segment)
              & refined[Filter.ValidName]    (Slash ~ Segment ~ PathEnd)) { (pname, pversion, fname) =>
        completeOrRecoverWith(deletePackageFilter(pname, pversion, fname)) {
          case MissingPackageFilterException =>
            complete(StatusCodes.NotFound -> ErrorRepresentation( ErrorCode("filter_not_found"), s"No filter with the name '$fname' defined for package $pname-$pversion" ))
          case e => failWith(e)
        }
      }
    }

  def route: Route =
    handleExceptions( ExceptionHandler( Errors.onMissingFilter orElse Errors.onMissingPackage ) ) {
      filterRoute ~ packageFiltersRoute ~ validateRoute
    }

  case object MissingPackageFilterException extends Throwable

  def deletePackageFilter(pname: Package.Name, pversion: Package.Version, fname: Filter.Name)
            (implicit db: Database) : Future[Unit] =
    db.run(PackageFilters.delete(pname, pversion, fname)).flatMap( i =>
      if (i == 0) FastFuture.failed(MissingPackageFilterException)
      else FastFuture.successful(()))


  def validateRoute: Route = {
    pathPrefix("validate") {
      path("filter") ((post & entity(as[Filter])) (_ => complete("OK")))
    }
  }

}
