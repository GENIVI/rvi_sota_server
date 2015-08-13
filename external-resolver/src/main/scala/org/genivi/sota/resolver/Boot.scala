/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import Function._
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route, PathMatchers}
import akka.stream.ActorMaterializer
import org.genivi.sota.resolver.db._
import org.genivi.sota.rest.Validation._
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}
import scala.concurrent.ExecutionContext
import scala.util.Try
import slick.jdbc.JdbcBackend.Database
import org.genivi.sota.resolver.types.Vehicle


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
      (put & refined[Vehicle.Vin](PathMatchers.Slash ~ PathMatchers.Segment ~ PathMatchers.PathEnd)) { vin =>
        complete(db.run( Vehicles.add(Vehicle(vin)) ).map(_ => NoContent))
      }
    }

  def packagesRoute: Route =
    pathPrefix("packages") {
      get {
        complete {
          NoContent
        }
      } ~
      (put & refined[Package.ValidName](PathMatchers.Slash ~ PathMatchers.Segment)
           & refined[Package.ValidVersion](PathMatchers.Slash ~ PathMatchers.Segment ~ PathMatchers.PathEnd)
           & entity(as[Package.Metadata]))
      { (name: Package.Name, version: Package.Version, metadata: Package.Metadata) =>
        complete(db.run(Packages.add(Package(Package.Id(name, version), metadata.description, metadata.vendor))))
      }
    }

  def resolveRoute: Route =
    path("resolve" / LongNumber) { pkgId =>
      get {
        complete {
          import org.genivi.sota.refined.SprayJsonRefined._
          db.run( Vehicles.list ).map( _.map(vehicle => Map(vehicle.vin -> List(pkgId)))
            .foldRight(Map[Vehicle.IdentificationNumber, List[Long]]())(_++_))
        }
      }
    }

  def filterRoute: Route =
    path("filters") {
      get {
        complete(db.run(Filters.list))
      } ~
      (post & entity(as[Filter])) { filter => complete(db.run(Filters.add(filter))) }
    }

  def validateRoute: Route =
    pathPrefix("validate") {
      path("filter") ((post & entity(as[Filter])) (const(complete("OK"))))
    }

  def packageFiltersRoute: Route =
    pathPrefix("packageFilter") {
      get {
        complete(db.run(PackageFilters.list))
      } ~
      (post & entity(as[PackageFilter])) { pf =>
        complete(db.run(PackageFilters.add(pf)))
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
