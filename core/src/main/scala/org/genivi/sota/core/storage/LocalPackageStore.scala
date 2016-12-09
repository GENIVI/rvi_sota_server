/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.storage

import java.io.File
import java.net.URI
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl.{FileIO, Sink, Source}
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
                              fileData: Source[ByteString, Any]): Sink[ByteString, Future[(Uri, PackageSize)]] = {
    val path = storePath.resolve(filename)
    val uri = Uri(path.toUri.toString)

    FileIO
      .toPath(path)
      .mapMaterializedValue(_.map(result => (uri, result.count)))
  }

  override def store(packageId: PackageId,
                     filename: String, fileData: Source[ByteString, Any]): Future[(Uri, PackageSize, DigestResult)] = {
    val sink = localFileSink(packageId, filename, fileData)
    writePackage(packageId, fileData, sink)
  }

  def retrieve(packageId: PackageId, packageUri: Uri): Future[(Uri, UniversalEntity)] = {
    val file = new File(new URI(packageUri.toString()))
    val size = file.length()
    val source = FileIO.fromPath(file.toPath)
    Future.successful {
      (packageUri, HttpEntity(MediaTypes.`application/octet-stream`, size, source))
    }
  }
}
