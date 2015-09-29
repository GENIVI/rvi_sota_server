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
import akka.http.scaladsl.server.ExceptionHandler.PF
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route, PathMatchers, Directive1}
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import cats.data.Xor
import eu.timepit.refined.Refined
import io.circe.generic.auto._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.resolver.db._
import org.genivi.sota.resolver.route._
import org.genivi.sota.resolver.types.{Vehicle, Package, Filter, PackageFilter}
import org.genivi.sota.rest.ErrorCode
import org.genivi.sota.rest.ErrorRepresentation
import org.genivi.sota.rest.Handlers.{rejectionHandler, exceptionHandler}
import org.genivi.sota.rest.Validation._
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NoStackTrace
import slick.jdbc.JdbcBackend.Database


object RefinementDirectives {
  import Directives._

  def refinedPackageId: Directive1[Package.Id] =
    (refined[Package.ValidName]   (Slash ~ Segment) &
     refined[Package.ValidVersion](Slash ~ Segment ~ PathEnd))
       .as[Package.Id](Package.Id.apply _)

}

object VehicleDirectives {
  import Directives._
  import RefinementDirectives._

  def installedPackagesHandler: PF = {
    case VehicleRoute.MissingVehicle =>
      complete(StatusCodes.NotFound ->
        ErrorRepresentation(Vehicle.MissingVehicle, "Vehicle doesn't exist"))

    case Errors.MissingPackageException =>
      complete(StatusCodes.NotFound ->
        ErrorRepresentation(Errors.Codes.PackageNotFound, "Package doesn't exist"))
  }

  def route(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Route = {
    pathPrefix("vehicles") {
      get {
        pathEnd {
          complete(db.run(Vehicles.list))
        }
      } ~
      (put & refined[Vehicle.ValidVin](Slash ~ Segment ~ PathEnd)) { vin =>
        pathEnd {
          complete(db.run( Vehicles.add(Vehicle(vin)) ).map(_ => NoContent))
        }
      } ~
      (get & refined[Vehicle.ValidVin](Slash ~ Segment))
      { vin =>
        path("package") {
          completeOrRecoverWith(VehicleRoute.packagesOnVin(vin)) {
            case VehicleRoute.MissingVehicle =>
              complete(StatusCodes.NotFound ->
                ErrorRepresentation(Vehicle.MissingVehicle, "Vehicle doesn't exist"))
          }
        }
      } ~
      (put & refined[Vehicle.ValidVin](Slash ~ Segment))
      { vin =>
        (pathPrefix("package") & refinedPackageId)
        { pkgId =>
          pathEnd {
            completeOrRecoverWith(VehicleRoute.installPackage(vin, pkgId)) {
              installedPackagesHandler
            }
          }
        }
      } ~
      (delete & refined[Vehicle.ValidVin](Slash ~ Segment))
      { vin =>
        (pathPrefix("package") & refinedPackageId)
        { pkgId =>
          pathEnd {
            completeOrRecoverWith(VehicleRoute.uninstallPackage(vin, pkgId)) {
              installedPackagesHandler
            }
          }
        }
      }
    }
  }
}

object FutureSupport {

  implicit class FutureOps[T]( x: Future[Option[T]] ) {

    def failIfNone( t: Throwable )
                  (implicit ec: ExecutionContext): Future[T] =
      x.flatMap( _.fold[Future[T]]( FastFuture.failed(t) )(FastFuture.successful) )

  }

}

object PackageDirectives {
  import Directives._
  import RefinementDirectives._

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
  import RefinementDirectives._
  import FutureSupport._
  import scala.concurrent.Future
  import org.genivi.sota.resolver.types.{True, And}
  import org.genivi.sota.resolver.types.{Vehicle, Package, FilterAST}
  import org.genivi.sota.resolver.types.FilterParser.parseValidFilter
  import org.genivi.sota.resolver.types.FilterQuery.query

  def makeFakeDependencyMap
    (name: Package.Name, version: Package.Version, vs: Seq[Vehicle])
      : Map[Vehicle.Vin, List[Package.Id]] =
    vs.map(vehicle => Map(vehicle.vin -> List(Package.Id(name, version))))
      .foldRight(Map[Vehicle.Vin, List[Package.Id]]())(_++_)

  def resolve(name: Package.Name, version: Package.Version)
              (implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Future[Map[Vehicle.Vin, Seq[Package.Id]]] = for {
    _       <- VehicleRoute.existsPackage(Package.Id(name, version))
    (p, fs) <- db.run(PackageFilters.listFiltersForPackage(Package.Id(name, version)))
    vs      <- db.run(Vehicles.list)
  } yield makeFakeDependencyMap(name, version, vs.filter(query(fs.map(_.expression).map(parseValidFilter).foldLeft[FilterAST](True)(And))))

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
        VehicleDirectives.route ~ PackageDirectives.route ~ new FilterDirectives().routes() ~ DependenciesDirectives.route
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
