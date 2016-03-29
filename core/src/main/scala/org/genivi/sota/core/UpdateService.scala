/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.event.LoggingAdapter
import akka.http.scaladsl.util.FastFuture
import cats.Show
import org.genivi.sota.core.data.{Download, Package, UpdateRequest, UpdateSpec, UpdateStatus}
import org.genivi.sota.core.db.{Packages, UpdateRequests, UpdateSpecs}
import org.genivi.sota.core.transfer.UpdateNotifier
import org.genivi.sota.data.{PackageId, Vehicle}
import slick.dbio.DBIO
import slick.driver.MySQLDriver.api.Database

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

case class PackagesNotFound(packageIds: (PackageId)*)
                           (implicit show: Show[PackageId])
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

import org.genivi.sota.core.rvi.{RviClient, ServerServices}
class UpdateService(registeredServices: ServerServices)(implicit val log: LoggingAdapter, rviClient: RviClient) {
  import UpdateService._

  def checkVins( dependencies: VinsToPackages ) : Future[Boolean] = FastFuture.successful( true )

  def mapIdsToPackages(vinsToDeps: VinsToPackages )
                      (implicit db: Database, ec: ExecutionContext): Future[Map[PackageId, Package]] = {
    def mapPackagesToIds( packages: Seq[Package] ) : Map[PackageId, Package] = packages.map( x => x.id -> x).toMap

    def missingPackages( required: Set[PackageId], found: Seq[Package] ) : Set[PackageId] = {
      val result = required -- found.map( _.id )
      if( result.nonEmpty ) log.debug( s"Some of required packages not found: $result" )
      result
    }

    log.debug(s"Dependencies from resolver: $vinsToDeps")
    val requirements : Set[PackageId]  =
      vinsToDeps.foldLeft(Set.empty[PackageId])((acc, vinDeps) => acc.union(vinDeps._2) )
    for {
      foundPackages <- db.run( Packages.byIds( requirements ) )
      mapping       <- if( requirements.size == foundPackages.size ) {
                         FastFuture.successful( mapPackagesToIds( foundPackages ) )
                       } else {
                         FastFuture.failed( PackagesNotFound( missingPackages(requirements, foundPackages).toArray: _*))
                       }
    } yield mapping

  }

  def loadPackage( id : PackageId)
                 (implicit db: Database, ec: ExecutionContext): Future[Package] = {
    db.run(Packages.byId(id)).flatMap { x =>
      x.fold[Future[Package]](FastFuture.failed( PackagesNotFound(id)) )(FastFuture.successful)
    }
  }

  def mkUploadSpecs(request: UpdateRequest, vinsToPackageIds: VinsToPackages,
                    idsToPackages: Map[PackageId, Package]): Set[UpdateSpec] = {
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
  type VinsToPackages = Map[Vehicle.Vin, Set[PackageId]]
  type DependencyResolver = Package => Future[VinsToPackages]

}
