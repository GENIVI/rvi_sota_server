/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.nio.file.{Path, Paths}
import java.security.MessageDigest

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.io.SynchronousFileSink
import akka.util.ByteString
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.apache.commons.codec.binary.Hex
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.{Packages, UpdateSpecs}
import org.genivi.sota.data.PackageId
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.rest.ErrorRepresentation
import org.genivi.sota.rest.Validation._
import slick.driver.MySQLDriver.api.Database

import scala.concurrent.Future
import scala.util.{Failure, Success}

class PackagesResource(resolver: ExternalResolverClient, db : Database)
                      (implicit system: ActorSystem, mat: ActorMaterializer) {

  import akka.stream.stage._
  import system.dispatcher

  private[this] val log = Logging.getLogger( system, "org.genivi.sota.core.PackagesResource" )

  /**
   * Streaming calculation of the hash of a stream.
   * This is used by savePackage (below)
   *
   * @param algorithm The hash algorithm, usually "SHA-1"
   * @return A stream that will calculate the hash of the contents fed through it
   */
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

  /**
   * The location on disk where uploaded package are stored.
   */
  val storePath : Path = Paths.get( system.settings.config.getString("upload.store") )

  import cats.syntax.show._

  /**
   * Handle an incomming file upload.
   * Write the contents to disk, while calculating the SHA-1 hash of the
   * package's contents
   *
   * @param packageId The name/version number of the package this is being uploaded
   * @param fileData A stream of the package contents
   * @return A tuple of the URI where the package is available, the package
   *         size and the package's checksum
   */
  def savePackage( packageId: PackageId, fileData: StrictForm.FileData ) : Future[(Uri, Long, String)] = {
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

  /**
   * A scala HTTP routing directive that extracts a package name/version from
   * the request URL
   */
  val extractPackageId : Directive1[PackageId] = (
    refined[PackageId.ValidName](Slash ~ Segment) &
    refined[PackageId.ValidVersion](Slash ~ Segment)).as(PackageId.apply _)

  import io.circe.generic.auto._
  val route = pathPrefix("packages") {
    (pathEnd & get) {
      parameters('regex.as[String Refined Regex].?) { (regex: Option[String Refined Regex]) =>
        import org.genivi.sota.marshalling.CirceMarshallingSupport._
        val query = (regex) match {
          case Some(r) => Packages.searchByRegex(r.get)
          case None => Packages.list
        }
        complete(db.run(query))
      }
    } ~
    extractPackageId { packageId =>
      pathEnd {
        get {
          // TODO: Include error description with rejectEmptyResponse?
          rejectEmptyResponse {
            import org.genivi.sota.marshalling.CirceMarshallingSupport._
            complete {
              db.run(Packages.byId(packageId))
            }
          }
        } ~
        put {
        // TODO: Fix form fields metadata causing error for large upload
        parameters('description.?, 'vendor.?, 'signature.?) { (description, vendor, signature) =>
          formFields('file.as[StrictForm.FileData]) { fileData =>
            completeOrRecoverWith(
              for {
                _                   <- resolver.putPackage(packageId, description, vendor)
                (uri, size, digest) <- savePackage(packageId, fileData)
                _                   <- db.run(Packages.create(
                                        Package(packageId, uri, size, digest, description, vendor, signature)))
              } yield StatusCodes.NoContent
            ) {
              case ExternalResolverRequestFailed(msg, cause) =>
                import org.genivi.sota.marshalling.CirceMarshallingSupport._
                log.error(cause, s"Unable to create/update package: $msg")
                complete(
                  StatusCodes.ServiceUnavailable ->
                  ErrorRepresentation(ErrorCodes.ExternalResolverError, msg))
              case e => failWith(e)
            }
          }
        }
        }
      } ~
      path("queued") {
        import org.genivi.sota.marshalling.CirceMarshallingSupport._
        complete(db.run(UpdateSpecs.getVinsQueuedForPackage(packageId.name, packageId.version)))
      }
    }
  }

}
