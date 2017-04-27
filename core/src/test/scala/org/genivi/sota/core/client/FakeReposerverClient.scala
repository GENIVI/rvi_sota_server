/*
 * Copyright: Copyright (C) 2017, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.client

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.data.Namespace
import com.advancedtelematic.libtuf.data.TufDataType.{Checksum, RepoId}
import com.advancedtelematic.libtuf.reposerver._
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._
import scala.concurrent.Future

object FakeReposerverClient extends ReposerverClient {

  private var store: ConcurrentHashMap[Namespace, RepoId] = new ConcurrentHashMap()

  def createRoot(namespace: Namespace): Future[RepoId] = {
    val repoId = RepoId(UUID.randomUUID())
    store.put(namespace, repoId)
    FastFuture.successful(repoId)
  }

  def addTarget(namespace: Namespace,
                fileName: String,
                uri: Uri,
                checksum: Checksum,
                length: Int): Future[Unit] =
    store.asScala.get(namespace) match {
      case Some(_) => FastFuture.successful(())
      case None    => FastFuture.failed(new Exception("not authorized"))
    }

}
