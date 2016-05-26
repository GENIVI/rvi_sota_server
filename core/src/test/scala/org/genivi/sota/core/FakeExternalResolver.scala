package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.stream.ActorMaterializer
import io.circe.Json
import org.genivi.sota.core.resolver.DefaultExternalResolverClient
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{PackageId, Vehicle}

import scala.concurrent.Future

class FakeExternalResolver()(implicit system: ActorSystem, mat: ActorMaterializer)
  extends DefaultExternalResolverClient(Uri.Empty, Uri.Empty, Uri.Empty, Uri.Empty)
{
  val installedPackages = scala.collection.mutable.Queue.empty[PackageId]

  val logger = Logging.getLogger(system, this)

  override def setInstalledPackages(vin: Vehicle.Vin, json: Json): Future[Unit] = {
    val ids = json
      .cursor
      .downField("packages")
      .map(_.as[List[PackageId]].getOrElse(List.empty))
      .toSeq
      .flatten

    installedPackages.enqueue(ids:_*)
    Future.successful(())
  }

  override def resolve(namespace: Namespace, packageId: PackageId): Future[Map[Vehicle, Set[PackageId]]] = {
    Future.successful(Map.empty)
  }

  override def handlePutResponse(futureResponse: Future[HttpResponse]): Future[Unit] =
    Future.successful(())

  override def putPackage(namespace: Namespace,
                          packageId: PackageId,
                          description: Option[String],
                          vendor: Option[String]): Future[Unit] = {
    logger.info(s"Fake resolver called. namespace=$namespace, packageId=${packageId.mkString}")
    Future.successful(())
  }
}
