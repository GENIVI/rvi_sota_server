/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route, PathMatchers}
import akka.stream.ActorMaterializer
import org.genivi.sota.refined.SprayJsonRefined._
import org.genivi.sota.resolver.db._
import org.genivi.sota.resolver.types.{Vehicle, Filter}
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import org.genivi.sota.rest.Validation._
import scala.concurrent.ExecutionContext
import scala.util.Try
import slick.jdbc.JdbcBackend.Database


class Routing(db: Database)
  (implicit system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext) extends Directives {

  import org.genivi.sota.resolver.types.{Vehicle$, Package, Filter, PackageFilter}
  import spray.json.DefaultJsonProtocol._
  import org.genivi.sota.rest.Handlers._

  def vehiclesRoute: Route =
    pathPrefix("vehicles") {
      get {
        complete(db.run(Vehicles.list))
      } ~
      (put & refined[Vehicle.ValidVin](PathMatchers.Slash ~ PathMatchers.Segment ~ PathMatchers.PathEnd)) { vin =>
        complete(db.run( Vehicles.add(Vehicle(vin)) ).map(_ => NoContent))
      }
    }

  def refinedPackageId =
    refined[Package.ValidName](PathMatchers.Slash ~ PathMatchers.Segment) &
      refined[Package.ValidVersion](PathMatchers.Slash ~ PathMatchers.Segment ~ PathMatchers.PathEnd)

  def packagesRoute: Route =
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

  def resolveRoute: Route =
    pathPrefix("resolve") {
      (get & refinedPackageId) { (name, version) =>
        complete(db.run(Resolve.resolve(name, version)))
      }
    }

  def packageFiltersHandler: ExceptionHandler = ExceptionHandler {
    case err: PackageFilters.MissingPackageException =>
      complete(StatusCodes.BadRequest ->
        ErrorRepresentation(PackageFilter.MissingPackage, "Package doesn't exist"))
    case err: Filters.MissingFilterException         =>
      complete(StatusCodes.BadRequest ->
        ErrorRepresentation(PackageFilter.MissingFilter, "Filter doesn't exist"))
  }

  def filterRoute: Route =
    pathPrefix("filters") {
      get {
        complete(db.run(Filters.list))
      } ~
      (post & entity(as[Filter])) { filter => complete(db.run(Filters.add(filter)))
      } ~
      (put  & refined[Filter.ValidName](PathMatchers.Slash ~ PathMatchers.Segment ~ PathMatchers.PathEnd)
            & entity(as[Filter.Expression])) { (fname, expr) =>
        handleExceptions(packageFiltersHandler) {
          complete(db.run(Filters.update(Filter(fname, expr))))
        }
      }
    }

  def validateRoute: Route =
    pathPrefix("validate") {
      path("filter") ((post & entity(as[Filter])) (_ => complete("OK")))
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
      (get & refined[Filter.ValidName](PathMatchers.Slash ~ PathMatchers.Segment ~ PathMatchers.PathEnd)) { fname =>
        complete(db.run(PackageFilters.listPackagesForFilter(fname)))
      }
    } ~
    pathPrefix("packageFilters" / "filtersFor") {
      (get & refinedPackageId) { (pname, pversion) =>
        complete(db.run(PackageFilters.listFiltersForPackage(pname, pversion)))
      }
    }
  }

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        vehiclesRoute ~ packagesRoute ~ resolveRoute ~ filterRoute ~ validateRoute ~ packageFiltersRoute
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
