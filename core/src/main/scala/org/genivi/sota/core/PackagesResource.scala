/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import cats.data.OptionT
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db._
import org.genivi.sota.core.resolver.{ExternalResolverClient, ExternalResolverRequestFailed}
import org.genivi.sota.core.storage.PackageStorage
import org.genivi.sota.core.storage.PackageStorage.PackageStorageOp
import org.genivi.sota.data.Namespace
import org.genivi.sota.data.PackageId
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.messaging.Messages.PackageCreated
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.rest.ErrorRepresentation
import org.genivi.sota.rest.Validation._
import slick.driver.MySQLDriver.api.Database
import org.genivi.sota.core.data.client.ResponseConversions._
import org.genivi.sota.core.data.PackageResponse._
import cats.std.future._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling._
import io.circe.generic.auto._

import scala.concurrent.Future

object PackagesResource {
  /**
    * A scala HTTP routing directive that extracts a package name/version from
    * the request URL
    */
  val extractPackageId: Directive1[PackageId] = (
    refined[PackageId.ValidName](Slash ~ Segment) &
      refined[PackageId.ValidVersion](Slash ~ Segment)).as(PackageId.apply _)
}

class PackagesResource(resolver: ExternalResolverClient, db : Database,
                       messageBusPublisher: MessageBusPublisher,
                       namespaceExtractor: Directive1[Namespace])
                      (implicit system: ActorSystem, mat: ActorMaterializer) {

  import system.dispatcher
  import PackagesResource._

  implicit val _config = system.settings.config
  implicit val _db = db

  private[this] val log = Logging.getLogger(system, "org.genivi.sota.core.PackagesResource")

  val packageStorageOp: PackageStorageOp = new PackageStorage().store _

  /**
    * An ota client GET a Seq of [[Package]] either from regex search, or from table scan.
    */
  def searchPackage(ns: Namespace): Route = {
    parameters('regex.as[String Refined Regex].?) { (regex: Option[String Refined Regex]) =>
      val query = Packages.searchByRegexWithBlacklist(ns, regex.map(_.get))

      val result = db.run(query).map { pkgs =>
        pkgs.map { case (pkg, blacklist) =>
          pkg.toResponse(blacklist)
        }
      }

      complete(result)
    }
  }

  /**
    * An ota client GET [[Package]] info, including for example S3 uri of its binary file.
    */
  def fetch(ns: Namespace, pid: PackageId): Route = {
    // TODO: Include error description with rejectEmptyResponse?
    rejectEmptyResponse {
      complete {
        val query = for {
          p <- Packages.byId(ns, pid)
          isBlacklisted <- BlacklistedPackages.isBlacklisted(ns, pid)
        } yield (p, isBlacklisted)

        db.run(query).map { case (pkg, blacklisted) =>
          pkg.toResponse(blacklisted)
        }
      }
    }
  }

  /**
    * An ota client PUT the given [[PackageId]] and uploads a binary file for it via form submission.
    * <ul>
    *   <li>Resolver is notified about the new package.</li>
    *   <li>That file is stored in S3 (obtaining an S3-URI in the process)</li>
    *   <li>A record for the package is inserted in Core's DB (Package SQL table).</li>
    * </ul>
    */
  def updatePackage(ns: Namespace, pid: PackageId): Route = {
    def storePackage(ns: Namespace, pid: PackageId,
                     description: Option[String], vendor: Option[String],
                     signature: Option[String],
                     fileName: String, file: Source[ByteString, Any]): Future[StatusCode] = {
      val resultF = for {
        _ <- resolver.putPackage(ns, pid, description, vendor)
        (uri, size, digest) <- packageStorageOp(pid, fileName, file)
        pkg <- db.run(Packages.create(Package(ns, pid, uri, size, digest, description, vendor, signature)))
      } yield StatusCodes.NoContent

      resultF.andThen {
        case scala.util.Success(_) =>
          messageBusPublisher.publish(PackageCreated(ns, pid, description, vendor, signature, fileName))
      }
    }

    def handleErrors(throwable: Throwable): Route = throwable match {
      case ExternalResolverRequestFailed(msg, cause) =>
        log.error(cause, s"Unable to create/update package: $msg")
        complete(StatusCodes.ServiceUnavailable -> ErrorRepresentation(ErrorCodes.ExternalResolverError, msg))
      case e => failWith(e)
    }

    // FIXME: There is a bug in akka, so we need to drain the stream before
    // returning the response
    def drainStream(file: Source[ByteString, Any]): Future[Done] = {
      file.runWith(Sink.ignore).recoverWith { case t =>
        log.warning(s"Could not drain stream: ${t.getMessage}")
        Future.successful(Done)
      }
    }

    // TODO: Fix form fields metadata causing error for large upload
    parameters('description.?, 'vendor.?, 'signature.?) { (description, vendor, signature) =>
      fileUpload("file") { case (fileInfo, file) =>
        val storePkgF = storePackage(ns, pid, description, vendor, signature, fileInfo.fileName, file)
        completeOrRecoverWith(storePkgF) { ex => onComplete(drainStream(file))(_ => handleErrors(ex)) }
      }
    }
  }

  case class PackageInfo(description: String)

  def updatePackageInfo(ns: Namespace, pid: PackageId): Route = {
    entity(as[PackageInfo]) { pi =>
      complete(db.run(Packages.updateInfo(ns,pid,pi.description)))
    }
  }
  /**
    * An ota client GET the devices waiting for the given [[Package]] to be installed.
    */
  def queuedDevices(ns: Namespace, pid: PackageId): Route = {
    complete(db.run(UpdateSpecs.getDevicesQueuedForPackage(ns, pid.name, pid.version)))
  }

  val route =
    pathPrefix("packages") {
      (get & namespaceExtractor & pathEnd) { ns =>
        searchPackage(ns)
      } ~
      (namespaceExtractor & extractPackageId) { (ns, pid) =>
        path("info") {
          put {
            updatePackageInfo(ns,pid)
          }
        } ~
        pathEnd {
          get {
            fetch(ns, pid)
          } ~
          put {
            updatePackage(ns, pid)
          }
        } ~
        path("queued") {
          queuedDevices(ns, pid)
        }
      }
    }
}
