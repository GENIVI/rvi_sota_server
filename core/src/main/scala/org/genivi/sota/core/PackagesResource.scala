/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.server.{Directive1, Route}
import akka.stream._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.core.common.NamespaceDirective._
import org.genivi.sota.core.data.Package
import org.genivi.sota.core.db.{Packages, UpdateSpecs}
import org.genivi.sota.core.resolver.{ExternalResolverClient, ExternalResolverRequestFailed}
import org.genivi.sota.core.storage.PackageStorage
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.PackageId
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.rest.ErrorRepresentation
import org.genivi.sota.rest.Validation._
import slick.driver.MySQLDriver.api.Database

object PackagesResource {
  /**
    * A scala HTTP routing directive that extracts a package name/version from
    * the request URL
    */
  val extractPackageId: Directive1[PackageId] = (
    refined[PackageId.ValidName](Slash ~ Segment) &
      refined[PackageId.ValidVersion](Slash ~ Segment)).as(PackageId.apply _)
}

class PackagesResource(resolver: ExternalResolverClient, db : Database)
                      (implicit system: ActorSystem, mat: ActorMaterializer) {

  import akka.stream.stage._
  import system.dispatcher
  import org.genivi.sota.marshalling.CirceMarshallingSupport._
  import PackagesResource._

  implicit val _config = system.settings.config

  private[this] val log = Logging.getLogger(system, "org.genivi.sota.core.PackagesResource")

  val packageStorage = new PackageStorage()

  def searchPackage(ns: Namespace): Route = {
    parameters('regex.as[String Refined Regex].?) { (regex: Option[String Refined Regex]) =>
      val query = regex match {
        case Some(r) => Packages.searchByRegex(ns, r.get)
        case None => Packages.list
      }
      complete(db.run(query))
    }
  }

  def fetch(ns: Namespace, pid: PackageId) = {
    // TODO: Include error description with rejectEmptyResponse?
    rejectEmptyResponse {
      complete {
        db.run(Packages.byId(ns, pid))
      }
    }
  }

  def updatePackage(ns: Namespace, pid: PackageId) = {
    // TODO: Fix form fields metadata causing error for large upload
    parameters('description.?, 'vendor.?, 'signature.?) { (description, vendor, signature) =>
      formFields('file.as[StrictForm.FileData]) { fileData =>
        completeOrRecoverWith(
          for {
            _ <- resolver.putPackage(ns, pid, description, vendor)
            (uri, size, digest) <- packageStorage.store(pid, fileData)
            _ <- db.run(Packages.create(
              Package(ns, pid, uri, size, digest, description, vendor, signature)))
          } yield StatusCodes.NoContent
        ) {
          case ExternalResolverRequestFailed(msg, cause) =>
            log.error(cause, s"Unable to create/update package: $msg")
            complete(
              StatusCodes.ServiceUnavailable ->
                ErrorRepresentation(ErrorCodes.ExternalResolverError, msg))
          case e => failWith(e)
        }
      }
    }
  }

  def queued(ns: Namespace, pid: PackageId) = {
    complete(db.run(UpdateSpecs.getVinsQueuedForPackage(ns, pid.name, pid.version)))
  }

  val route =
    pathPrefix("packages") {
      (get & extractNamespace & pathEnd) { ns =>
        searchPackage(ns)
      } ~
      (extractNamespace & extractPackageId) { (ns, pid) =>
        pathEnd {
          get {
            fetch(ns, pid)
          } ~
          put {
            updatePackage(ns, pid)
          }
        } ~
        path("queued") {
          queued(ns, pid)
        }
      }
    }
}
