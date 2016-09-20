package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.stream.ActorMaterializer
import io.circe.Json
import org.genivi.sota.core.resolver.{DefaultExternalResolverClient, ExternalResolverClient}
import org.genivi.sota.data.Device.Id
import org.genivi.sota.data.{Device, Namespace, PackageId}

import scala.concurrent.Future

class FakeExternalResolver()(implicit system: ActorSystem, mat: ActorMaterializer)
  extends ExternalResolverClient
{
  import org.genivi.sota.marshalling.CirceInstances._

  private val installedPackages = scala.collection.mutable.Queue.empty[PackageId]

  private val logger = Logging.getLogger(system, this)

  override def setInstalledPackages(device: Device.Id, json: Json): Future[Unit] = {
    val ids = json
      .cursor
      .downField("packages")
      .map(_.as[List[PackageId]].getOrElse(List.empty))
      .toSeq
      .flatten

    installedPackages.enqueue(ids:_*)
    Future.successful(())
  }

  override def resolve(namespace: Namespace, packageId: PackageId): Future[Map[Device.Id, Set[PackageId]]] = {
    Future.successful(Map.empty)
  }

  override def putPackage(namespace: Namespace,
                          packageId: PackageId,
                          description: Option[String],
                          vendor: Option[String]): Future[Unit] = {
    logger.info(s"Fake resolver called. namespace=$namespace, packageId=${packageId.mkString}")
    Future.successful(())
  }

  override def affectedDevices(packageIds: Set[PackageId]): Future[Map[Id, Seq[PackageId]]] =
    Future.successful(Map.empty)

  def isInstalled(packageId: PackageId): Boolean = installedPackages.contains(packageId)
}
