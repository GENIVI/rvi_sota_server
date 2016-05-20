/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core.storage

import java.io.File
import java.net.URI
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl.{FileIO, Sink}
import akka.util.ByteString
import org.genivi.sota.core.DigestCalculator.DigestResult
import org.genivi.sota.data.PackageId
import scala.concurrent.Future


class LocalPackageStore()(implicit val system: ActorSystem, val mat: ActorMaterializer) extends PackageStore {
  import PackageStorage._
  import system.dispatcher

  val storePath = Paths.get(system.settings.config.getString("upload.store"))

  val log = Logging.getLogger(system, this)

  protected def localFileSink(packageId: PackageId, filename: String,
                              fileData: StrictForm.FileData): Sink[ByteString, Future[(Uri, PackageSize)]] = {
    val file = storePath.resolve(filename).toFile
    val uri = Uri(file.toURI.toString)

    FileIO
      .toFile(file)
      .mapMaterializedValue(_.map(result => (uri, result.count)))
  }

  override def store(packageId: PackageId, fileData: StrictForm.FileData): Future[(Uri, PackageSize, DigestResult)] = {
    val fileName = packageFileName(packageId, fileData.filename)
    val sink = localFileSink(packageId, fileName, fileData)
    val dataStream = fileData.entity.dataBytes
    writePackage(packageId, dataStream, sink)
  }

  def retrieve(packageId: PackageId, packageUri: Uri): Future[(Uri, UniversalEntity)] = {
    val file = new File(new URI(packageUri.toString()))
    val size = file.length()
    val source = FileIO.fromFile(file)
    Future.successful {
      (packageUri, HttpEntity(MediaTypes.`application/octet-stream`, size, source))
    }
  }
}
