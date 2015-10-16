/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.event.LoggingAdapter
import akka.http.scaladsl.util.FastFuture
import cats.Show
import java.util.UUID
import org.genivi.sota.core.data.{Package, Vehicle}
import org.genivi.sota.core.data.{UpdateRequest, UpdateSpec, Download, UpdateStatus}
import org.genivi.sota.core.transfer.UpdateNotifier
import scala.concurrent.ExecutionContext

import scala.concurrent.Future
import org.genivi.sota.core.db.{Packages, UpdateRequests, UpdateSpecs}
import scala.util.control.NoStackTrace
import slick.dbio.DBIO
import slick.driver.MySQLDriver.api.Database

case class PackagesNotFound(packageIds: (Package.Id)*)
                           (implicit show: Show[Package.Id])
    extends Throwable(s"""Package(s) not found: ${packageIds.map(show.show).mkString(", ")}""") with NoStackTrace

case class UploadConf( chunkSize: Int, downloadSplitStrategy: Set[Package] => Vector[Download] )

object UploadConf {

  implicit val default = UploadConf(
    chunkSize = 64 * 1024,
    downloadSplitStrategy = packages => {
      packages.map(p => Download.apply(Vector(p))).toVector
    }
  )

}

import org.genivi.sota.core.rvi.{ServerServices, RviClient}
class UpdateService(registeredServices: ServerServices)(implicit val log: LoggingAdapter, rviClient: RviClient) {
  import UpdateService._

  def checkVins( dependencies: VinsToPackages ) : Future[Boolean] = FastFuture.successful( true )

  def mapIdsToPackages(vinsToDeps: VinsToPackages )
                      (implicit db: Database, ec: ExecutionContext): Future[Map[Package.Id, Package]] = {
    def mapPackagesToIds( packages: Seq[Package] ) : Map[Package.Id, Package] = packages.map( x => x.id -> x).toMap

    def missingPackages( required: Set[Package.Id], found: Seq[Package] ) : Set[Package.Id] = {
      val result = required -- found.map( _.id )
      if( result.nonEmpty ) log.debug( s"Some of required packages not found: $result" )
      result
    }

    log.debug(s"Dependencies from resolver: $vinsToDeps")
    val requirements : Set[Package.Id]  =
      vinsToDeps.foldLeft(Set.empty[Package.Id])((acc, vinDeps) => acc.union(vinDeps._2) )
    for {
      foundPackages <- db.run( Packages.byIds( requirements ) )
      mapping       <- if( requirements.size == foundPackages.size ) {
                         FastFuture.successful( mapPackagesToIds( foundPackages ) )
                       } else {
                         FastFuture.failed( PackagesNotFound( missingPackages(requirements, foundPackages).toArray: _*))
                       }
    } yield mapping

  }

  def loadPackage( id : Package.Id)
                 (implicit db: Database, ec: ExecutionContext): Future[Package] = {
    db.run(Packages.byId(id)).flatMap { x =>
      x.fold[Future[Package]](FastFuture.failed( PackagesNotFound(id)) )(FastFuture.successful)
    }
  }

  def mkUploadSpecs(request: UpdateRequest, vinsToPackageIds: VinsToPackages,
                    idsToPackages: Map[Package.Id, Package]): Set[UpdateSpec] = {
    vinsToPackageIds.map {
      case (vin, requiredPackageIds) =>
        val packages : Set[Package] = requiredPackageIds.map( idsToPackages.get ).map( _.get )
        UpdateSpec( request, vin, UpdateStatus.Pending, packages)
    }.toSet
  }

  def persistRequest(request: UpdateRequest, updateSpecs: Set[UpdateSpec])
                    (implicit db: Database, ec: ExecutionContext) : Future[Unit] = {
    db.run(
      DBIO.seq( UpdateRequests.persist(request) +: updateSpecs.map( UpdateSpecs.persist ).toArray: _*)).map( _ => ()
    )
  }

  def queueUpdate(request: UpdateRequest, resolver : DependencyResolver )
                 (implicit db: Database, ec: ExecutionContext): Future[Set[UpdateSpec]] = {
    for {
      pckg           <- loadPackage(request.packageId)
      vinsToDeps     <- resolver(pckg)
      packages       <- mapIdsToPackages(vinsToDeps)
      updateSpecs    = mkUploadSpecs(request, vinsToDeps, packages)
      _              <- persistRequest(request, updateSpecs)
      _              <- Future.successful( UpdateNotifier.notify(updateSpecs.toSeq, registeredServices) )
    } yield updateSpecs
  }

  def all(implicit db: Database, ec: ExecutionContext): Future[Set[UpdateRequest]] =
    db.run(UpdateRequests.list).map(_.toSet)
}

object UpdateService {
  type VinsToPackages = Map[Vehicle.Vin, Set[Package.Id]]
  type DependencyResolver = Package => Future[VinsToPackages]

}
