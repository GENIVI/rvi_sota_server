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
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import eu.timepit.refined.Refined
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPackageId
import org.genivi.sota.resolver.db._
import org.genivi.sota.resolver.types.{Package, Filter, PackageFilter}
import org.genivi.sota.resolver.vehicle._
import org.genivi.sota.rest.Handlers.{rejectionHandler, exceptionHandler}
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NoStackTrace
import slick.jdbc.JdbcBackend.Database


object PackageDirectives {
  import Directives._

  def route(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Route = {

    pathPrefix("packages") {
      get {
        complete {
          NoContent
        }
      } ~
      (put & refinedPackageId & entity(as[Package.Metadata]))
      { (id, metadata) =>
        val pkg = Package(id, metadata.description, metadata.vendor)
        complete(db.run(Packages.add(pkg).map(_ => pkg)))
      }
    }
  }
}

object DependenciesDirectives {
  import Directives._
  import scala.concurrent.Future
  import org.genivi.sota.resolver.types.{True, And}
  import org.genivi.sota.resolver.types.{Package, FilterAST}
  import org.genivi.sota.resolver.types.FilterParser.parseValidFilter
  import org.genivi.sota.resolver.types.FilterQuery.query

  def makeFakeDependencyMap
    (name: Package.Name, version: Package.Version, vs: Seq[Vehicle])
      : Map[Vehicle.Vin, List[Package.Id]] =
    vs.map(vehicle => Map(vehicle.vin -> List(Package.Id(name, version))))
      .foldRight(Map[Vehicle.Vin, List[Package.Id]]())(_++_)

  def resolve(name: Package.Name, version: Package.Version)
              (implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Future[Map[Vehicle.Vin, Seq[Package.Id]]] = for {
    _       <- VehicleFunctions.existsPackage(Package.Id(name, version))
    (p, fs) <- db.run(PackageFilters.listFiltersForPackage(Package.Id(name, version)))
    vs      <- db.run(VehicleDAO.list)
    ps : Seq[Seq[Package.Id]]                   <- Future.sequence(vs.map(v => VehicleFunctions.packagesOnVin(v.vin)))
    vps: Seq[Tuple2[Vehicle, Seq[Package.Id]]]  =  vs.zip(ps)
  } yield makeFakeDependencyMap(name, version,
            vps.filter(query(fs.map(_.expression).map(parseValidFilter).foldLeft[FilterAST](True)(And)))
               .map(_._1))

  def route(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Route = {
    pathPrefix("resolve") {
      (get & refinedPackageId) { id =>
        completeOrRecoverWith(resolve(id.name, id.version)) {
          Errors.onMissingPackage
        }
      }
    }
  }
}

class Routing(implicit db: Database, system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext)
    extends Directives {

  val route: Route = pathPrefix("api" / "v1") {
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        new VehicleDirectives().route ~ PackageDirectives.route ~ new FilterDirectives().routes() ~ DependenciesDirectives.route
      }
    }
  }
}

object Boot extends App {

  implicit val system       = ActorSystem("sota-external-resolver")
  implicit val materializer = ActorMaterializer()
  implicit val exec         = system.dispatcher
  implicit val log          = Logging(system, "boot")
  implicit val db           = Database.forConfig("database")

  log.info(org.genivi.sota.resolver.BuildInfo.toString)

  val route         = new Routing
  val host          = system.settings.config.getString("server.host")
  val port          = system.settings.config.getInt("server.port")
  val bindingFuture = Http().bindAndHandle(route.route, host, port)

  log.info(s"Server online at http://${host}:${port}/")

  sys.addShutdownHook {
    Try( db.close()  )
    Try( system.shutdown() )
  }
}
