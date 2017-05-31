/*
 * Copyright: Copyright (C) 2017, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.core.client

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.util.FastFuture
import com.advancedtelematic.libats.data.Namespace
import com.advancedtelematic.libtuf.data.TufDataType.{Checksum, HardwareIdentifier, RepoId, TargetName, TargetVersion}
import com.advancedtelematic.libtuf.reposerver._
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction

import com.advancedtelematic.libtuf.data.TufDataType

import scala.collection.JavaConverters._
import scala.concurrent.Future

object FakeReposerverClient extends ReposerverClient {

  case class FakeTarget(namespace: Namespace, fileName: String, uri: Uri, checksum: Checksum,
                        length: Int, name: Option[TargetName],
                        version: Option[TargetVersion],
                        hardwareIds: Seq[HardwareIdentifier])

  private var store: ConcurrentHashMap[Namespace, RepoId] = new ConcurrentHashMap()
  private var targets: ConcurrentHashMap[RepoId, Seq[FakeTarget]] = new ConcurrentHashMap()

  def createRoot(namespace: Namespace): Future[RepoId] = {
    val repoId = RepoId(UUID.randomUUID())
    store.put(namespace, repoId)
    FastFuture.successful(repoId)
  }

  override def addTarget(namespace: Namespace, fileName: String, uri: Uri, checksum: Checksum,
                         length: Int, name: Option[TargetName], version: Option[TargetVersion],
                         hardwareIds: Seq[HardwareIdentifier]): Future[Unit] =
    store.asScala.get(namespace) match {
      case Some(repoId) =>
        val newFakeTarget = FakeTarget(namespace, fileName, uri, checksum, length, name, version, hardwareIds)

        targets.compute(repoId, new BiFunction[RepoId, Seq[FakeTarget], Seq[FakeTarget]] {
          override def apply(repo: RepoId, old: Seq[FakeTarget]): Seq[FakeTarget] =
            newFakeTarget +: Option(old).toSeq.flatten
        })

        FastFuture.successful(())
      case None => FastFuture.failed(new Exception("not authorized"))
    }

  def targets(repoId: RepoId): Seq[FakeTarget] = targets.getOrDefault(repoId, Seq.empty)
}
