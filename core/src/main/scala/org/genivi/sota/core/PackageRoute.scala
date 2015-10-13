/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.io.SynchronousFileSink
import akka.util.ByteString
import eu.timepit.refined.Refined
import eu.timepit.refined.string.Regex
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.rest.{ErrorRepresentation}
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import slick.driver.MySQLDriver.api.Database
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.Packages
import akka.http.scaladsl.server.Directives._
import org.genivi.sota.rest.Validation._

class PackagesResource(resolver: ExternalResolverClient, db : Database)
                      (implicit system: ActorSystem, mat: ActorMaterializer) {

  import akka.stream.stage._
  import system.dispatcher

  private[this] val log = Logging.getLogger( system, "org.genivi.sota.core.PackagesResource" )

  def digestCalculator(algorithm: String) : PushPullStage[ByteString, String] = new PushPullStage[ByteString, String] {
    val digest = MessageDigest.getInstance(algorithm)

    override def onPush(chunk: ByteString, ctx: Context[String]): SyncDirective = {
      digest.update(chunk.toArray)
      ctx.pull()
    }

    override def onPull(ctx: Context[String]): SyncDirective = {
      if (ctx.isFinishing) ctx.pushAndFinish( Hex.encodeHexString(digest.digest()))
      else ctx.pull()
    }

    override def onUpstreamFinish(ctx: Context[String]): TerminationDirective = {
      // If the stream is finished, we need to emit the last element in the onPull block.
      // It is not allowed to directly emit elements from a termination block
      // (onUpstreamFinish or onUpstreamFailure)
      ctx.absorbTermination()
    }
  }

  val storePath : Path = Paths.get( system.settings.config.getString("upload.store") )

  import cats.syntax.show._
  def savePackage( packageId: Package.Id, fileData: StrictForm.FileData ) : Future[(Uri, Long, String)] = {
    val fileName = fileData.filename.getOrElse(s"${packageId.name.get}-${packageId.version.get}")
    val file = storePath.resolve(fileName).toFile
    val data = fileData.entity.dataBytes
    val uploadResult : Future[(Uri, Long, String)] = for {
      size <- data.runWith( SynchronousFileSink( file ) )
      digest <- data.transform(() => digestCalculator("SHA-1")).runFold("")( (acc, data) => acc ++ data)
    } yield (file.toURI().toString(), size, digest)

    uploadResult.onComplete {
      case Success((uri, size, digest)) =>
        log.debug( s"Package ${packageId.show} uploaded to $uri. Check sum: $digest")
      case Failure(t) =>
        log.error(t, s"Failed to save package ${packageId.show}")
    }
    uploadResult
  }

  import io.circe.generic.auto._
  val route = pathPrefix("packages") {
    get {
      parameters('regex.as[String Refined Regex].?) { (regex: Option[String Refined Regex]) =>
        import org.genivi.sota.marshalling.CirceMarshallingSupport._
        val query = (regex) match {
          case Some(r) => Packages.searchByRegex(r.get)
          case None => Packages.list
        }
        complete(db.run(query))
      }
    } ~
    (put & refined[Package.ValidName]( Slash ~ Segment) & refined[Package.ValidVersion](Slash ~ Segment ~ PathEnd))
        .as(Package.Id.apply _) { packageId =>
      formFields('description.?, 'vendor.?, 'file.as[StrictForm.FileData]) { (description, vendor, fileData) =>
        completeOrRecoverWith(
          for {
            _                   <- resolver.putPackage(packageId, description, vendor)
            (uri, size, digest) <- savePackage(packageId, fileData)
            _                   <- db.run(Packages.create( Package(packageId, uri, size, digest, description, vendor) ))
          } yield StatusCodes.NoContent
        ) {
          case ExternalResolverRequestFailed(msg, cause) =>
            import org.genivi.sota.marshalling.CirceMarshallingSupport._
            log.error( cause, s"Unable to create/update package: $msg" )
            complete( StatusCodes.ServiceUnavailable -> ErrorRepresentation( ErrorCodes.ExternalResolverError, msg ) )

          case e => failWith(e)
        }
      }
    }
  }

}
