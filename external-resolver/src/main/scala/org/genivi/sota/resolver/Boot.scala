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
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import org.genivi.sota.resolver.Validation._
import org.genivi.sota.resolver.rest.ErrorCodes
import org.genivi.sota.resolver.rest.ErrorRepresentation
import org.genivi.sota.resolver.db._
import scala.concurrent.ExecutionContext
import scala.util.Try
import slick.jdbc.JdbcBackend.Database


class Routing(db: Database)
  (implicit system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext) extends Directives {

  import org.genivi.sota.resolver.types.{Vin, Package, Filter}
  import spray.json.DefaultJsonProtocol._
  import org.genivi.sota.resolver.rest.RejectionHandlers._

  def vinsRoute: Route =
    pathPrefix("vins") {
      get {
        complete(db.run(Vins.list))
      } ~
      vput[Vin](s => Right(Vin(s))) { vin: Vin.ValidVin => db.run(Vins.add(vin)).map(_ => NoContent) }
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
          db.run( Vins.list ).map( _.map(vin => Map(vin.vin -> List(pkgId)))
            .foldRight(Map[String, List[Long]]())(_++_))
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
      path("vin")     (vpost[Vin]    (const("OK"))) ~
      // path("package") (vpost[Package](const("OK"))) ~
      path("filter")  (vpost[Filter] (const("OK")))
    }

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      vinsRoute ~ packagesRoute ~ resolveRoute ~ filterRoute ~ validateRoute
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
