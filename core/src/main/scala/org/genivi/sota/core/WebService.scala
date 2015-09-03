/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.io.File
import java.security.MessageDigest

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model.{StatusCodes, Uri, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.PathMatchers.Slash
import akka.http.scaladsl.server.{PathMatchers, ExceptionHandler, Directives}
import akka.parboiled2.util.Base64
import akka.stream.ActorMaterializer
import akka.stream.io.SynchronousFileSink
import akka.util.ByteString
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.{Packages, Vehicles, InstallRequests, InstallCampaigns}
import org.genivi.sota.rest.{ErrorCode, ErrorRepresentation}
import org.genivi.sota.rest.Validation._
import slick.driver.MySQLDriver.api.Database
import scala.concurrent.Future
import Directives._
import eu.timepit.refined._
import eu.timepit.refined.string._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import org.genivi.sota.refined.SprayJsonRefined._

object ErrorCodes {
  val ExternalResolverError = ErrorCode( "external_resolver_error" )
}

class VehiclesResource(db: Database)
                      (implicit system: ActorSystem, mat: ActorMaterializer) {

  import system.dispatcher

  val route = pathPrefix("vehicles") {
    (put & refined[Vehicle.Vin](PathMatchers.Slash ~ PathMatchers.Segment ~ PathMatchers.PathEnd)) { vin =>
          complete(db.run( Vehicles.create(Vehicle(vin)) ).map(_ => NoContent))
        }
    } ~
    path("vehicles") {
      get {
        parameters('regex.?) { (regex) =>
          val query = regex match {
            case Some(r) => Vehicles.searchByRegex(r)
            case _ => Vehicles.list()
          }
          complete{ db.run(query) }
        }
      }
    }
}

class CampaignsResource(resolver: ExternalResolverClient, db: Database)
                       (implicit system: ActorSystem, mat: ActorMaterializer) {
  import system.dispatcher

  def createCampaign(campaign: InstallCampaign): Future[InstallCampaign] = {
    def persistCampaign(dependencies: Map[Vehicle, Set[PackageId]]) = for {
      persistedCampaign <- InstallCampaigns.create(campaign)
      _ <- InstallRequests.createRequests(InstallRequest.from(dependencies, persistedCampaign.id.head).toSeq)
    } yield persistedCampaign

    for {
      dependencyMap <- resolver.resolve(campaign.packageId)
      persistedCampaign <- db.run(persistCampaign(dependencyMap))
    } yield persistedCampaign
  }

  val route = path("install_campaigns") {
    (post & entity(as[InstallCampaign])) { campaign =>
      complete( createCampaign( campaign ) )
    }
  }
}

class PackagesResource(resolver: ExternalResolverClient, db : Database)
                      (implicit system: ActorSystem, mat: ActorMaterializer) {

  import akka.stream.stage._
  import system.dispatcher

  private[this] val log = Logging.getLogger( system, "packagesResource" )

  def digestCalculator(algorithm: String) : PushPullStage[ByteString, String] = new PushPullStage[ByteString, String] {
    val digest = MessageDigest.getInstance(algorithm)

    override def onPush(chunk: ByteString, ctx: Context[String]): SyncDirective = {
      digest.update(chunk.toArray)
      ctx.pull()
    }

    override def onPull(ctx: Context[String]): SyncDirective = {
      if (ctx.isFinishing) ctx.pushAndFinish(Base64.rfc2045().encodeToString(digest.digest(), false))
      else ctx.pull()
    }

    override def onUpstreamFinish(ctx: Context[String]): TerminationDirective = {
      // If the stream is finished, we need to emit the last element in the onPull block.
      // It is not allowed to directly emit elements from a termination block
      // (onUpstreamFinish or onUpstreamFailure)
      ctx.absorbTermination()
    }
  }

  def savePackage( packageId: PackageId, fileData: StrictForm.FileData )(implicit system: ActorSystem, mat: ActorMaterializer) : Future[(Uri, Long, String)] = {
    val fileName = fileData.filename.getOrElse(s"${packageId.name.get}-${packageId.version.get}")
    val file = new File(fileName)
    val data = fileData.entity.dataBytes
    for {
      size <- data.runWith( SynchronousFileSink( file ) )
      digest <- data.transform(() => digestCalculator("SHA-1")).runFold("")( (acc, data) => acc ++ data)
    } yield (file.toURI().toString(), size, digest)
  }

  val route = pathPrefix("packages") {
    get {
      parameters('regex.as[String Refined Regex].?) { (regex: Option[String Refined Regex]) =>

        val query = (regex) match {
          case Some(r) => Packages.searchByRegex(r.get)
          case None => Packages.list
      }
        complete(db.run(query))
      }
    } ~
    (put & refined[Package.ValidName]( Slash ~ Segment) & refined[Package.ValidVersion](Slash ~ Segment ~ PathEnd)).as(PackageId.apply _) { packageId =>
      formFields('description.?, 'vendor.?, 'file.as[StrictForm.FileData]) { (description, vendor, fileData) =>
        completeOrRecoverWith(
          for {
            _                   <- resolver.putPackage(packageId, description, vendor)
            (uri, size, digest) <- savePackage(packageId, fileData)
            _                   <- db.run(Packages.create( Package(packageId, uri, size, digest, description, vendor) ))
          } yield NoContent
        ) {
          case ExternalResolverRequestFailed(msg, cause) => {
            import org.genivi.sota.CirceSupport._
            import io.circe.generic.auto._
            import akka.http.scaladsl.unmarshalling._
            log.error( cause, s"Unable to create/update package: $msg" )
            complete( StatusCodes.ServiceUnavailable -> ErrorRepresentation( ErrorCodes.ExternalResolverError, msg ) )
          }
          case e => failWith(e)
        }
      }
    }
  }

}

class WebService(resolver: ExternalResolverClient, db : Database)
                (implicit system: ActorSystem, mat: ActorMaterializer) extends Directives {
  val log = Logging(system, "webservice")

  import io.circe.Json
  import Json.{obj, string}

  val exceptionHandler = ExceptionHandler {
    case e: Throwable =>
      extractUri { uri =>
        log.error(s"Request to $uri errored: $e")
        val entity = obj("error" -> string(e.getMessage()))
        complete(HttpResponse(InternalServerError, entity = entity.toString()))
      }
  }

  val vehicles = new VehiclesResource( db )
  val campaigns = new CampaignsResource(resolver, db)
  val packages = new PackagesResource(resolver, db)

  val route = pathPrefix("api" / "v1") {
    handleExceptions(exceptionHandler) {
       vehicles.route ~ campaigns.route ~ packages.route
    }
  }

}
