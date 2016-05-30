/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.storage

import java.io.{File, InputStream}

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.common.StrictForm.FileData
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl.{FileIO, StreamConverters}
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import com.typesafe.config.Config
import org.genivi.sota.core.DigestCalculator.DigestResult
import org.genivi.sota.core.data.Package
import org.genivi.sota.data.PackageId
import org.joda.time.{DateTime, Period}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.Try

class S3PackageStore(credentials: S3Credentials)
                    (implicit val system: ActorSystem, val mat: ActorMaterializer) extends PackageStore {
  import PackageStorage._
  import system.dispatcher

  val PUBLIC_URL_EXPIRE_TIME = Period.days(1)

  val bucketId = credentials.bucketId

  lazy val s3client = {
    val client = new AmazonS3Client(credentials)
    client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
    client
  }

  override def store(packageId: PackageId, fileData: FileData): Future[(Uri, PackageSize, DigestResult)] = {
    val filename = packageFileName(packageId, fileData.filename)

    val sink = StreamConverters.asInputStream()
      .mapMaterializedValue { is => upload(is, filename, fileData) }

    writePackage(packageId, fileData.entity.dataBytes, sink)
  }

  protected def signedUri(packageId: PackageId, uri: Uri): Future[Uri] = {
    val expire = DateTime.now.plus(PUBLIC_URL_EXPIRE_TIME).toDate
    val filename = Try(uri.path.reverse.head.toString).getOrElse("/404")
    val f = Future { s3client.generatePresignedUrl(bucketId, filename, expire)}
    f map (uri => Uri(uri.toURI.toString))
  }

  protected def upload(is: InputStream, fileName: String, fileData: FileData): Future[(Uri, Long)] = {
    val metadata = new ObjectMetadata()
    metadata.setContentLength(fileData.entity.contentLength)

    val request = new PutObjectRequest(bucketId, fileName, is, metadata)
      .withCannedAcl(CannedAccessControlList.AuthenticatedRead)

    val asyncPut = for {
      putResult <- Future { s3client.putObject(request) }
      url <- Future { s3client.getUrl(bucketId, fileName) }
    } yield (putResult, url)

    asyncPut map { case (putResult, url)  =>
      (Uri(url.toString), putResult.getMetadata.getContentLength)
    }
  }

  override def retrieve(packageId: PackageId, packageUri: Uri): Future[(Uri, UniversalEntity)] = {
    signedUri(packageId, packageUri).map((_, HttpEntity.Empty))
  }

  def retrieveFile(packageUri: Uri): Future[File] = {
    def asyncFileDownload(fileKey: String, toFile: File) = Future {
      val req = new GetObjectRequest(bucketId, fileKey)
      s3client.getObject(req, toFile)
    }

    val file = File.createTempFile("s3storage-download", ".tmp")

    for {
      key <- Future.fromTry(Try(packageUri.path.reverse.head.toString))
      _ <- asyncFileDownload(key, file)
    } yield file
  }
}

object S3PackageStore {
  lazy val logger = LoggerFactory.getLogger(this.getClass)

  def apply(config: Config)
           (implicit system: ActorSystem, mat: ActorMaterializer): S3PackageStore = {
    val credentials = loadCredentials(config).get
    new S3PackageStore(credentials)
  }

  protected[core] def loadCredentials(config: Config): Option[S3Credentials] = {
    val t = for {
      accessKey <- Try(config.getString("core.s3.accessKey"))
      secretKey <- Try(config.getString("core.s3.secretKey"))
      bucketId <- Try(config.getString("core.s3.bucketId"))
    } yield {
      val result = List(accessKey, secretKey, bucketId)

      if(result.forall(_.nonEmpty)) {
        Option(new S3Credentials(accessKey, secretKey, bucketId))
      } else {
        logger.warn("Could not initialize s3 credentials with empty parameters")
        None
      }
    }

    t.toOption.flatten
  }
}


class S3Credentials(accessKey: String, secretKey: String, val bucketId: String) extends AWSCredentials {
  override def getAWSAccessKeyId: String = accessKey

  override def getAWSSecretKey: String = secretKey
}

