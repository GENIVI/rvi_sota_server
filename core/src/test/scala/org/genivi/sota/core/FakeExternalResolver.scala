package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import io.circe.Json
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.data.{Namespace, PackageId, Uuid}

import scala.concurrent.Future

class FakeExternalResolver()(implicit system: ActorSystem, mat: ActorMaterializer)
  extends ExternalResolverClient
{
  import org.genivi.sota.marshalling.CirceInstances._

  private val installedPackages = scala.collection.mutable.Queue.empty[PackageId]

  private val logger = Logging.getLogger(system, this)

  override def setInstalledPackages(device: Uuid, json: Json): Future[Unit] = {
    val ids = json
      .cursor
      .downField("packages")
      .map(_.as[List[PackageId]].getOrElse(List.empty))
      .toSeq
      .flatten

    installedPackages.enqueue(ids:_*)
    Future.successful(())
  }

  override def resolve(namespace: Namespace, packageId: PackageId): Future[Map[Uuid, Set[PackageId]]] = {
    Future.successful(Map.empty)
  }

  def isInstalled(packageId: PackageId): Boolean = installedPackages.contains(packageId)
}
