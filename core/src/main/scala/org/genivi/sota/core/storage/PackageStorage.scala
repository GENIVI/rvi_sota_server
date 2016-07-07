/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.storage

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.common.StrictForm
import akka.stream._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import com.typesafe.config.Config
import org.genivi.sota.core.DigestCalculator
import org.genivi.sota.core.DigestCalculator.DigestResult
import org.genivi.sota.core.storage.PackageStorage.PackageSize
import org.genivi.sota.data.PackageId
import org.genivi.sota.core.data.Package
import akka.http.scaladsl.model._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class PackageStorage()(implicit system: ActorSystem, mat: ActorMaterializer, config: Config) {
  def store(packageId: PackageId, fileData: StrictForm.FileData): Future[(Uri, PackageSize, DigestResult)] = {
    storage.store(packageId, fileData)
  }

  def retrieveResponse(packageModel: Package): Future[HttpResponse] = {
    import system.dispatcher

    if(isS3Uri(packageModel.uri)) {
      s3Storage.retrieve(packageModel.id, packageModel.uri) map { case (uri, _) =>
        HttpResponse(StatusCodes.Found, headers.Location(uri) :: Nil)
      }
    } else {
      new LocalPackageStore().retrieve(packageModel.id, packageModel.uri) map { case (_, entity) =>
        HttpResponse(StatusCodes.OK, entity = entity)
      }
    }
  }

  private def isS3Uri(uri: Uri) = {
    uri.scheme.startsWith("https") &&
      uri.authority.host.address().endsWith("amazonaws.com")
  }

  protected[storage] lazy val storage: PackageStore = {
    S3PackageStore.loadCredentials(config) match {
      case Some(c) => new S3PackageStore(c)
      case None => new LocalPackageStore()
    }
  }

  private lazy val s3Storage: S3PackageStore = {
    S3PackageStore.loadCredentials(config) match {
      case Some(c) => new S3PackageStore(c)
      case None => throw new Exception("Could not get s3 credentials from config")
    }
  }
}

object PackageStorage {
  import cats.syntax.show._
  type PackageSize = Long
  type PackageRetrievalOp = Package => Future[HttpResponse]
  type PackageStorageOp = (PackageId, StrictForm.FileData) => Future[(Uri, PackageSize, DigestResult)]

  private val HASH_ALGORITHM = "SHA-1"

  protected[storage] def writePackage(packageId: PackageId,
                                      fileData: Source[ByteString, NotUsed],
                                      sink: Sink[ByteString, Future[(Uri, PackageSize)]])
                  (implicit system: ActorSystem, mat: ActorMaterializer): Future[(Uri, PackageSize, DigestResult)] = {
    implicit val ec = system.dispatcher
    val log = Logging.getLogger(system, this)
    val digestCalculator = DigestCalculator(HASH_ALGORITHM)

    val (digestF, resultF) = fileData
      .alsoToMat(digestCalculator)(Keep.right)
      .toMat(sink)(Keep.both)
      .run()

    val writeAsync = for {
      digest <- digestF
      (uri, sizeBytes) <- resultF
    } yield (uri, sizeBytes, digest)

    writeAsync andThen logResult(log, packageId)
  }

  protected[storage] def packageFileName(packageId: PackageId, providedFilename: Option[String]) = {
    packageId.mkString + "-" + providedFilename.getOrElse("")
  }

  private def logResult(log: LoggingAdapter, packageId: PackageId)
  : PartialFunction[Try[(Uri, PackageSize, DigestResult)], Unit] = {
    case Success((uri, size, digest)) =>
      log.debug(s"Package ${packageId.show} uploaded to $uri. Check sum: $digest")
    case Failure(t) =>
      log.error(t, s"Failed to save package ${packageId.show}")
  }
}

trait PackageStore {
  def store(packageId: PackageId, fileData: StrictForm.FileData): Future[(Uri, PackageSize, DigestResult)]

  def retrieve(packageId: PackageId, packageUri: Uri): Future[(Uri, UniversalEntity)]
}

