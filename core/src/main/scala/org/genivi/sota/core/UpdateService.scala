/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.util.FastFuture
import cats.Show
import eu.timepit.refined.string._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.data._
import org.genivi.sota.core.db._
import org.genivi.sota.core.resolver.Connectivity
import org.genivi.sota.core.transfer.UpdateNotifier
import org.genivi.sota.data.{Namespace, PackageId, Uuid}
import java.time.Instant
import java.util.UUID

import org.genivi.sota.core.db.UpdateSpecs.UpdateSpecRow
import org.genivi.sota.messaging.{MessageBusPublisher, Messages}

import scala.collection.immutable.ListSet
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

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

class UpdateService(notifier: UpdateNotifier, deviceRegistry: DeviceRegistry)
                   (implicit val system: ActorSystem, val connectivity: Connectivity, val ec: ExecutionContext) {

  import UpdateService._

  implicit private val log = Logging(system, "updateservice")

  def checkDevices( dependencies: DeviceToPackageIds ) : Future[Boolean] = FastFuture.successful( true )

  /**
    * Fetch from DB the [[Package]]s corresponding to the given [[PackageId]]s,
    * failing in case not all could be fetched.
    */
  def fetchPackages(ns: Namespace, requirements: Set[PackageId] )
                   (implicit db: Database, ec: ExecutionContext): Future[Seq[Package]] = {

    def missingPackages( required: Set[PackageId], found: Seq[Package] ) : Set[PackageId] = {
      val result = required -- found.map( _.id )
      if( result.nonEmpty ) log.debug( s"Some of required packages not found: $result" )
      result
    }

    for {
      foundPackages <- db.run(Packages.byIds(ns, requirements))
      mapping       <- if( requirements.size == foundPackages.size ) {
                         FastFuture.successful( foundPackages )
                       } else {
                         FastFuture.failed( PackagesNotFound( missingPackages(requirements, foundPackages).toArray: _*))
                       }
    } yield mapping

  }

  def fetchUpdateSpecRows(id: Uuid)(implicit db: Database, ec: ExecutionContext): Future[Seq[UpdateSpecRow]] =
    db.run(UpdateSpecs.listUpdatesById(id))


  def loadPackage(id: UUID)
                 (implicit db: Database, ec: ExecutionContext): Future[Package] = {
    val dbIO = Packages.byUuid(id).flatMap(BlacklistedPackages.ensureNotBlacklisted)
    db.run(dbIO)
  }

  /**
    * For each of the given (VIN, dependencies) prepare an [[UpdateSpec]]
    * that points to the given [[UpdateRequest]] and has [[UpdateStatus]] "Pending".
    * <p>
    * No install order is specified for the single [[UpdateSpec]] that is prepared per VIN.
    * However, a timestamp is included in each [[UpdateSpec]] to break ties
    * with any other (already persisted) [[UpdateSpec]]s that might be pending.
    *
    * @param vinsToPackageIds several VIN-s and the dependencies for each of them
    * @param idsToPackages lookup a [[Package]] by its [[PackageId]]
    */
  def mkUpdateSpecs(request: UpdateRequest,
                    vinsToPackageIds: DeviceToPackageIds,
                    idsToPackages: Map[PackageId, Package]): Set[UpdateSpec] = {
    vinsToPackageIds.map {
      case (device, requiredPackageIds) =>
        UpdateSpec.default(request, device).copy(dependencies = requiredPackageIds.map(idsToPackages))
    }.toSet
  }

  def persistRequest(request: UpdateRequest, updateSpecs: Set[UpdateSpec])
                    (implicit db: Database, ec: ExecutionContext) : Future[Unit] = {
    val updateReqIO = UpdateRequests.persist(request)
    val updateSpecsIO = DBIO.sequence(updateSpecs.map(UpdateSpecs.persist).toSeq)

    val dbIO = updateReqIO.andThen(updateSpecsIO).map(_ => ())

    db.run(dbIO.withPinnedSession.transactionally)
  }

  /**
    * <ul>
    *   <li>For the [[Package]] of the given [[UpdateRequest]] find the vehicles where it needs to be installed,</li>
    *   <li>For each such VIN create an [[UpdateSpec]]</li>
    *   <li>Persist in DB all of the above</li>
    * </ul>
    */
  def queueUpdate(namespace: Namespace, request: UpdateRequest, resolver: DependencyResolver,
                  messageBus: MessageBusPublisher)
                 (implicit db: Database, ec: ExecutionContext): Future[Set[UpdateSpec]] = {
    val us = for {
      pckg           <- loadPackage(request.packageUuid)
      vinsToDeps     <- resolver(pckg)
      requirements    = allRequiredPackages(vinsToDeps)
      packages       <- fetchPackages(namespace, requirements)
      _ <- db.run(BlacklistedPackages.ensureNotBlacklistedIds(namespace)(packages.map(_.id)))
      idsToPackages   = packages.map( x => x.id -> x ).toMap
      updateSpecs     = mkUpdateSpecs(request, vinsToDeps, idsToPackages)
      _              <- persistRequest(request, updateSpecs)
      _              <- Future.successful(notifier.notify(updateSpecs.toSeq))
    } yield updateSpecs

    us.foreach { updateSpecs =>
      updateSpecs.foreach { updateSpec =>
        messageBus.publishSafe(Messages.UpdateSpec(namespace, updateSpec.device,
          updateSpec.request.packageUuid, updateSpec.status, updateSpec.updateTime))
      }
    }

    us
  }

  def allRequiredPackages(deviceToDeps: Map[Uuid, Set[PackageId]]): Set[PackageId] = {
    log.debug(s"Dependencies from resolver: $deviceToDeps")
    deviceToDeps.values.flatten.toSet
  }

  def updateRequest(ns: Namespace, packageId: PackageId)
                   (implicit db: Database, ec: ExecutionContext): Future[UpdateRequest] =
    db.run(Packages.byId(ns, packageId).flatMap(BlacklistedPackages.ensureNotBlacklisted)).map { p =>
      val newUpdateRequest = UpdateRequest.default(ns, p.uuid)
      newUpdateRequest.copy(signature = p.signature.getOrElse(newUpdateRequest.signature),
                            description = p.description)
    }

  /**
    * For the given [[PackageId]] and vehicle, persist a fresh [[UpdateRequest]] and a fresh [[UpdateSpec]].
    * Resolver is not contacted.
    */
  def queueDeviceUpdate(ns: Namespace, device: Uuid, packageId: PackageId)
                        (implicit db: Database, ec: ExecutionContext): Future[(UpdateRequest, UpdateSpec, Instant)] = {
    for {
      updateRequest <- updateRequest(ns, packageId)
      spec = UpdateSpec.default(updateRequest, device)
      _ <- persistRequest(updateRequest, ListSet(spec))
    } yield (updateRequest, spec, spec.updateTime)
  }

  def all(namespace: Namespace)(implicit db: Database, ec: ExecutionContext): Future[Seq[UpdateRequest]] =
    db.run(UpdateRequests.list(namespace))
}

object UpdateService {
  type DeviceToPackageIds = Map[Uuid, Set[PackageId]]
  type DependencyResolver = Package => Future[DeviceToPackageIds]
}
