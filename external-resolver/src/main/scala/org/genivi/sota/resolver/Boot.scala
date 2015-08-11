/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import akka.http.scaladsl.server.PathMatchers
import org.genivi.sota.rest.{Validation, ErrorCodes, ErrorRepresentation}

import Function._
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import Validation._
import org.genivi.sota.rest.{ErrorCodes, ErrorRepresentation}
import org.genivi.sota.resolver.db._
import scala.concurrent.ExecutionContext
import scala.util.Try
import slick.jdbc.JdbcBackend.Database
import org.genivi.sota.resolver.types.Vehicle


class Routing(db: Database)
  (implicit system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext) extends Directives {

  import org.genivi.sota.resolver.types.{Vehicle$, Package, Filter}
  import spray.json.DefaultJsonProtocol._
  import org.genivi.sota.rest.Handlers._

  def vehiclesRoute: Route =
    pathPrefix("vehicles") {
      get {
        complete(db.run(Vehicles.list))
      } ~
      (put & refined[String, Vehicle.Vin](PathMatchers.Segment)) { vin =>
        complete(db.run( Vehicles.add(Vehicle(vin)) ).map(_ => NoContent))
      }
    }

  def packagesRoute: Route =
    path("packages") {
      get {
        complete {
          NoContent
        }
      } ~
      (post & entity(as[Package])) { pkg: Package =>
        completeOrRecoverWith(db.run(Packages.add(pkg))) {
          case err: java.sql.SQLIntegrityConstraintViolationException if err.getErrorCode == 1062 =>
            complete(StatusCodes.Conflict ->
              ErrorRepresentation(ErrorCodes.DuplicateEntry, s"Package ${pkg.name}-${pkg.version} already exists"))
          case t =>
            failWith(t)
        }
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
      vpost[Filter] { filter => db.run(Filters.add(filter)) }
    }

  def validateRoute: Route =
    pathPrefix("validate") {
      path("filter")  (vpost[Filter] (const("OK")))
    }

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        vehiclesRoute ~ packagesRoute ~ resolveRoute ~ filterRoute ~ validateRoute
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
