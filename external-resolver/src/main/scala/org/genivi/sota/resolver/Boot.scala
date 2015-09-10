/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route, PathMatchers}
import akka.http.scaladsl.server.PathMatchers.Slash
import akka.stream.ActorMaterializer
import eu.timepit.refined.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.CirceSupport._
import org.genivi.sota.resolver.db._
import org.genivi.sota.resolver.types.{Vehicle, Package, Filter, PackageFilter}
import org.genivi.sota.rest.ErrorRepresentation.errorRepresentationEncoder
import org.genivi.sota.rest.Handlers.{rejectionHandler, exceptionHandler}
import org.genivi.sota.rest.Validation._
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import scala.concurrent.ExecutionContext
import scala.util.Try
import slick.jdbc.JdbcBackend.Database


class Routing(db: Database)
  (implicit system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext) extends Directives {

  def vehiclesRoute: Route = {
    pathPrefix("vehicles") {
      get {
        complete(db.run(Vehicles.list))
      } ~
      (put & refined[Vehicle.ValidVin](Slash ~ Segment ~ PathEnd)) { vin =>
        complete(db.run( Vehicles.add(Vehicle(vin)) ).map(_ => NoContent))
      }
    }
  }

  def refinedPackageId =
    refined[Package.ValidName](Slash ~ Segment) &
      refined[Package.ValidVersion](Slash ~ Segment ~ PathEnd)

  def packagesRoute: Route = {

    pathPrefix("packages") {
      get {
        complete {
          NoContent
        }
      } ~
      (put & refinedPackageId & entity(as[Package.Metadata])) { (name, version, metadata) =>
        complete(db.run(Packages.add(Package(Package.Id(name, version), metadata.description, metadata.vendor))))
      }
    }
  }

  def resolveRoute: Route = {
    pathPrefix("resolve") {
      (get & refinedPackageId) { (name, version) =>
        complete(db.run(Resolve.resolve(name, version)))
      }
    }
  }

  def packageFiltersHandler: ExceptionHandler = ExceptionHandler {
    case err: PackageFilters.MissingPackageException =>
      complete(StatusCodes.BadRequest ->
        ErrorRepresentation(PackageFilter.MissingPackage, "Package doesn't exist"))
    case err: PackageFilters.MissingPackageFilterException =>
      complete(StatusCodes.BadRequest ->
        ErrorRepresentation(PackageFilter.MissingPackageFilter, "Package filter doesn't exist"))
    case err: Filters.MissingFilterException         =>
      complete(StatusCodes.BadRequest ->
        ErrorRepresentation(PackageFilter.MissingFilter, "Filter doesn't exist"))
  }

  import org.genivi.sota.refined.SprayJsonRefined.refinedUnmarshaller

  def filterRoute: Route =
    pathPrefix("filters") {
      get {
        parameters('regex.as[Refined[String, Regex]].?) { re =>
          val query = re.fold(Filters.list)(r => Filters.searchByRegex(r.get))
          complete(db.run(query))
        }
      } ~
      (post & entity(as[Filter])) { filter => complete(db.run(Filters.add(filter)))
      } ~
      (put  & refined[Filter.ValidName](Slash ~ Segment ~ PathEnd)
            & entity(as[Filter.ExpressionWrapper])) { (fname, expr) =>
        handleExceptions(packageFiltersHandler) {
          complete(db.run(Filters.update(Filter(fname, expr.expression))))
        }
      } ~
      (delete & refined[Filter.ValidName](Slash ~ Segment ~ PathEnd)) { fname =>
        handleExceptions(packageFiltersHandler) {
          complete(db.run(Filters.delete(fname)))
        }
      }
    }

  def validateRoute: Route = {
    pathPrefix("validate") {
      path("filter") ((post & entity(as[Filter])) (_ => complete("OK")))
    }
  }

  def packageFiltersRoute: Route = {

    path("packageFilters") {
      get {
        complete(db.run(PackageFilters.list))
      } ~
      (post & entity(as[PackageFilter])) { pf =>
        handleExceptions(packageFiltersHandler) {
          complete(db.run(PackageFilters.add(pf)))
        }
      }
    } ~
    pathPrefix("packageFilters" / "packagesFor") {
      (get & refined[Filter.ValidName](Slash ~ Segment ~ PathEnd)) { fname =>
        complete(db.run(PackageFilters.listPackagesForFilter(fname)))
      }
    } ~
    pathPrefix("packageFilters" / "filtersFor") {
      (get & refinedPackageId) { (pname, pversion) =>
        complete(db.run(PackageFilters.listFiltersForPackage(pname, pversion)))
      }
    }
  }

  def packageFilterDeleteRoute: Route =
    pathPrefix("packageFiltersDelete") {
      (delete & refined[Package.ValidName](Slash ~ Segment)
              & refined[Package.ValidVersion](Slash ~ Segment)
              & refined[Filter.ValidName](Slash ~ Segment ~ PathEnd)) { (pname, pversion, fname) =>
        handleExceptions(packageFiltersHandler) {
          complete(db.run(PackageFilters.delete(pname, pversion, fname)))
        }
      }
    }

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        vehiclesRoute ~ packagesRoute ~ resolveRoute ~ filterRoute ~ validateRoute ~ packageFiltersRoute ~ packageFilterDeleteRoute
      }
    }
  }
}

object Boot extends App {

  implicit val system       = ActorSystem("sota-external-resolver")
  implicit val materializer = ActorMaterializer()
  implicit val exec         = system.dispatcher
  implicit val log          = Logging(system, "boot")

  log.info(org.genivi.sota.resolver.BuildInfo.toString)

  val db = Database.forConfig("database")

  val route         = new Routing(db)
  val host          = system.settings.config.getString("server.host")
  val port          = system.settings.config.getInt("server.port")
  val bindingFuture = Http().bindAndHandle(route.route, host, port)

  log.info(s"Server online at http://${host}:${port}/")

  sys.addShutdownHook {
    Try( db.close()  )
    Try( system.shutdown() )
  }
}
