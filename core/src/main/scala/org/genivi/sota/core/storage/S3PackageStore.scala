/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.storage

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl.{FileIO, Source}
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import com.typesafe.config.Config
import org.genivi.sota.core.DigestCalculator.DigestResult
import org.genivi.sota.data.PackageId
import java.time.{Duration, Instant}

import akka.util.ByteString
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.Try

class S3PackageStore(credentials: S3Credentials)
                    (implicit val system: ActorSystem, val mat: ActorMaterializer) extends PackageStore {
  import PackageStorage._
  import system.dispatcher

  val PUBLIC_URL_EXPIRE_TIME = Duration.ofDays(1)

  val bucketId = credentials.bucketId

  val log = LoggerFactory.getLogger(this.getClass)

  lazy val s3client = {
    val client = new AmazonS3Client(credentials)
    client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
    client
  }

  override def store(packageId: PackageId,
                     filename: String, fileData: Source[ByteString, Any]): Future[(Uri, PackageSize, DigestResult)] = {
    val tempFile = File.createTempFile(filename, ".tmp")

    // The s3 sdk requires us to specify the file size if using a stream
    // so we always need to cache the file into the filesystem before uploading
    val sink = FileIO.toPath(tempFile.toPath)
      .mapMaterializedValue {
        _.flatMap { result =>
          if(result.wasSuccessful) {
            upload(tempFile, filename, fileData)
              .andThen { case _ =>
                Try(tempFile.delete())
              }
          } else
            Future.failed(result.getError)
        }
      }

    writePackage(packageId, fileData, sink)
  }

  protected def signedUri(packageId: PackageId, uri: Uri): Future[Uri] = {
    val expire = java.util.Date.from(Instant.now.plus(PUBLIC_URL_EXPIRE_TIME))
    val filename = Try(uri.path.reverse.head.toString).getOrElse("/404")
    val f = Future { s3client.generatePresignedUrl(bucketId, filename, expire)}
    f map (uri => Uri(uri.toURI.toString))
  }

  protected def upload(file: File, fileName: String, fileData: Source[ByteString, Any]): Future[(Uri, Long)] = {
    val request = new PutObjectRequest(bucketId, fileName, file)
      .withCannedAcl(CannedAccessControlList.AuthenticatedRead)

    log.info(s"Uploading $fileName to amazon s3")

    val asyncPut = for {
      putResult <- Future { s3client.putObject(request) }
      url <- Future { s3client.getUrl(bucketId, fileName) }
    } yield (putResult, url)

    asyncPut map { case (putResult, url)  =>
      log.info(s"$fileName uploaded to $url")
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

