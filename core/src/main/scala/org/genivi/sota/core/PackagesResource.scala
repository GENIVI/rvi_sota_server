/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import java.util.UUID
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.data.PackageResponse._
import org.genivi.sota.core.db._
import org.genivi.sota.core.storage.PackageStorage
import org.genivi.sota.core.storage.PackageStorage.PackageStorageOp
import org.genivi.sota.core.storage.StoragePipeline
import org.genivi.sota.data.{Namespace, PackageId}
import org.genivi.sota.http.{AuthedNamespaceScope, ErrorHandler, Scopes}
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.messaging.MessageBusPublisher
import org.genivi.sota.rest.ResponseConversions._
import org.genivi.sota.rest.Validation._
import scala.concurrent.Future
import slick.driver.MySQLDriver.api.Database

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

class PackagesResource(updateService: UpdateService,
                       db: Database,
                       messageBusPublisher: MessageBusPublisher,
                       namespaceExtractor: Directive1[AuthedNamespaceScope])
                      (implicit system: ActorSystem, mat: ActorMaterializer) {

  import system.dispatcher
  import PackagesResource._

  implicit val _config = system.settings.config
  implicit val _db = db
  implicit val _bus = messageBusPublisher

  private[this] val log = Logging.getLogger(system, "org.genivi.sota.core.PackagesResource")

  val packageStorageOp: PackageStorageOp = new PackageStorage().store _
  lazy val storagePipeline = new StoragePipeline(updateService)

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
    *   <li>That file is stored in S3 (obtaining an S3-URI in the process)</li>
    *   <li>A record for the package is inserted in Core's DB (Package SQL table).</li>
    * </ul>
    */
  def updatePackage(ns: Namespace, pid: PackageId): Route = {

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

        val action = for {
          (uri, size, digest) <- packageStorageOp(pid, ns.get, file)
          pkg = Package(ns, UUID.randomUUID(), pid, uri, size, digest, description, vendor, signature)
          _ <- storagePipeline.storePackage(pkg)
        } yield ()

        completeOrRecoverWith(action.map(_ => StatusCodes.NoContent)) { ex =>
          onComplete(drainStream(file))(_ => failWith(ex))
        }
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
