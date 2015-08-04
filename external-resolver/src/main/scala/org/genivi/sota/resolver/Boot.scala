/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import org.genivi.sota.resolver.db._
import org.genivi.sota.resolver.Validation._
import scala.concurrent.ExecutionContext
import scala.util.Try
import slick.jdbc.JdbcBackend.Database


class Route(db: Database)
  (implicit system: ActorSystem, mat: ActorMaterializer, exec: ExecutionContext) extends Directives {

  import org.genivi.sota.resolver.types.{Vin, Package}
  import spray.json.DefaultJsonProtocol._
  import org.genivi.sota.resolver.rest.RejectionHandlers._

  val route = pathPrefix("api" / "v1") {
    handleRejections( rejectionHandler ) {
      pathPrefix("vins") {
        get {
          complete {
            NoContent
          }
        } ~
        (put & validatedPut(Vin.apply)) { vin: Vin.ValidVin =>
          complete(db.run(Vins.add(vin)).map(_ => NoContent))
        }
      } ~
      path("packages") {
        get {
          complete {
            NoContent
          }
        } ~
        (post & validated[Package]) { newPackage: Package.ValidPackage =>
          complete( db.run( Packages.add(newPackage) )
            .map( nid => newPackage.copy( id = Some(nid))) )
        }
      } ~
      path("resolve" / LongNumber) { pkgId =>
        complete {
          db.run( Vins.list ).map( _.map(vin => Map(vin.vin -> List(pkgId)))
            .foldRight(Map[String, List[Long]]())(_++_))
        }
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

  val route         = new Route(db)
  val host          = system.settings.config.getString("server.host")
  val port          = system.settings.config.getInt("server.port")
  val bindingFuture = Http().bindAndHandle(route.route, host, port)

  log.info(s"Server online at http://${host}:${port}/")

  sys.addShutdownHook {
    Try( db.close()  )
    Try( system.shutdown() )
  }
}
