/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core

import java.time.Instant
import java.util.UUID

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import org.genivi.sota.core.autoinstall.AutoInstall
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db._
import org.genivi.sota.core.resolver.{ExternalResolverClient, ExternalResolverRequestFailed}
import org.genivi.sota.core.storage.PackageStorage
import org.genivi.sota.core.storage.PackageStorage.PackageStorageOp
import org.genivi.sota.data.{Namespace, PackageId}
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.messaging.Messages.{PackageCreated, PackageStorageUsage}
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.rest.Validation._
import slick.driver.MySQLDriver.api.Database
import org.genivi.sota.rest.ResponseConversions._
import org.genivi.sota.core.data.PackageResponse._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import io.circe.generic.auto._
import org.genivi.sota.core.SotaCoreErrors.SotaCoreErrorCodes
import org.genivi.sota.http.{AuthedNamespaceScope, ErrorHandler, Scopes}
import org.genivi.sota.http.Errors.RawError

import scala.concurrent.Future

object PackagesResource {

  val extractPackageName: Directive1[PackageId.Name] =
    refined[PackageId.ValidName](Slash ~ Segment)

  /**
    * A scala HTTP routing directive that extracts a package name/version from
    * the request URL
    */
  val extractPackageId: Directive1[PackageId] =
    (extractPackageName & refined[PackageId.ValidVersion](Slash ~ Segment)).as(PackageId.apply _)
}

class PackagesResource(resolver: ExternalResolverClient,
                       updateService: UpdateService,
                       db : Database,
                       messageBusPublisher: MessageBusPublisher,
                       namespaceExtractor: Directive1[AuthedNamespaceScope])
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

      val result = db.run(query).map(_.toResponse)

      complete(result)
    }
  }

  /**
    * An ota client GET [[Package]] info, including for example S3 uri of its binary file.
    */
  def fetch(ns: Namespace, pid: PackageId): Route = {
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
                     file: Source[ByteString, Any]): Future[StatusCode] = {
      val packageCreateF = for {
        _ <- resolver.putPackage(ns, pid, description, vendor)
        (uri, size, digest) <- packageStorageOp(pid, ns.get, file)
        newPkg = Package(ns, UUID.randomUUID(), pid, uri, size, digest, description, vendor, signature)
        usage <- db.run(Packages.create(newPkg).andThen(Packages.usage(ns)))
      } yield usage

      for {
        usage <- packageCreateF
        _ <- AutoInstall.packageCreated(ns, pid, updateService)
        _ <- messageBusPublisher.publishSafe(PackageCreated(ns, pid, description, vendor, signature))
        _ <- messageBusPublisher.publishSafe(PackageStorageUsage(ns, Instant.now, usage))
      } yield StatusCodes.NoContent
    }

    def handleErrors(throwable: Throwable): Route = throwable match {
      case ExternalResolverRequestFailed(msg, cause) =>
        log.error(cause, s"Unable to create/update package: $msg")
        failWith(RawError(SotaCoreErrorCodes.ExternalResolverError, StatusCodes.ServiceUnavailable, msg))
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
      fileUpload("file") { case (_, file) =>
        val storePkgF = storePackage(ns, pid, description, vendor, signature, file)
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
    complete(db.run(UpdateSpecs.getDevicesQueuedForPackage(ns, pid)))
  }


  val route = (ErrorHandler.handleErrors & namespaceExtractor) { ns =>
    val scope = Scopes.packages(ns)
    pathPrefix("packages") {
      (scope.get & pathEnd) {
        searchPackage(ns)
      } ~
      extractPackageId { pid =>
        path("info") {
          scope.put {
            updatePackageInfo(ns, pid)
          }
        } ~
        pathEnd {
          scope.get {
            fetch(ns, pid)
          } ~
          scope.put {
            updatePackage(ns, pid)
          }
        } ~
        (scope.check & path("queued")) {
          queuedDevices(ns, pid)
        }
      }
    }
  }
}
