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
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route}
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import eu.timepit.refined.Refined
import cats.data.Xor
import io.circe.generic.auto._
import org.genivi.sota.resolver.db._
import org.genivi.sota.resolver.types.{Vehicle, Package, Filter, PackageFilter}
import org.genivi.sota.rest.ErrorCode
import org.genivi.sota.rest.ErrorRepresentation
import org.genivi.sota.rest.Handlers.{rejectionHandler, exceptionHandler}
import org.genivi.sota.rest.Validation._
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try
import org.genivi.sota.rest.SotaError
import org.genivi.sota.rest.SotaError._
import slick.jdbc.JdbcBackend.Database
import org.genivi.sota.marshalling.CirceMarshallingSupport._

object Errors {
  import Directives.complete
  import akka.http.scaladsl.server.ExceptionHandler.PF

  object MissingPackageException extends SotaError

  object MissingFilterException extends SotaError

  def onMissingFilter : PF = {
    case Errors.MissingFilterException => complete( StatusCodes.NotFound -> ErrorRepresentation( ErrorCode("filter_not_found"), s"Filter not found") )
  }

  def onMissingPackage : PF = {
    case Errors.MissingPackageException => complete( StatusCodes.NotFound -> ErrorRepresentation( ErrorCode("package_not_found"), "Package not found") )
  }


}

object VehicleDirectives {
  import Directives._

  def route(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Route = {
    pathPrefix("vehicles") {
      get {
        complete(db.run(Vehicles.list))
      } ~
        (put & refined[Vehicle.ValidVin](Slash ~ Segment ~ PathEnd)) { vin =>
        complete(db.run( Vehicles.add(Vehicle(vin)) ).map(_ => NoContent))
      }
    }
  }

}

object RefinementDirectives {
  import Directives._

  def refinedPackageId =
    refined[Package.ValidName](Slash ~ Segment) & refined[Package.ValidVersion](Slash ~ Segment ~ PathEnd)
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
      (put & refinedPackageId & entity(as[Package.Metadata])) { (name, version, metadata) =>
        complete(db.run(Packages.add(Package(Package.Id(name, version), metadata.description, metadata.vendor))))
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

  def resolve(name: Package.Name, version: Package.Version)
              (implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Future[Map[Vehicle.Vin, Seq[Package.Id]]] = for {
    _       <- db.run(Packages.load(name, version)).failIfNone( Errors.MissingPackageException )
    (p, fs) <- db.run(PackageFilters.listFiltersForPackage(Package.Id(name, version)))
    vs      <- db.run(Vehicles.list)
  } yield Resolve.makeFakeDependencyMap(name, version, vs.filter(query(fs.map(_.expression).map(parseValidFilter).foldLeft[FilterAST](True)(And))))

  def route(implicit db: Database, mat: ActorMaterializer, ec: ExecutionContext): Route = {
    pathPrefix("resolve") {
      (get & refinedPackageId) { (name, version) =>
        completeOrRecoverWith(resolve(name, version)) {
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

  log.info(org.genivi.sota.resolver.BuildInfo.toString)

  implicit val db   = Database.forConfig("database")

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
