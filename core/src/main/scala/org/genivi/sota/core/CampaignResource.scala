/**
  * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
  * License: MPL-2.0
  */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import cats.data.Xor
import io.circe.generic.auto._
import org.genivi.sota.common.DeviceRegistry
import org.genivi.sota.core.campaigns.{CampaignLauncher, CampaignStats}
import org.genivi.sota.core.data.Campaign
import org.genivi.sota.core.db.Campaigns
import org.genivi.sota.data.{Namespace, PackageId}
import org.genivi.sota.http.{AuthedNamespaceScope, ErrorHandler, Scopes}
import org.genivi.sota.http.UuidDirectives._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.messaging.MessageBusPublisher
import slick.driver.MySQLDriver.api.Database

import scala.concurrent.Future

class CampaignResource(namespaceExtractor: Directive1[AuthedNamespaceScope],
                       deviceRegistry: DeviceRegistry, updateService: UpdateService,
                       messageBus: MessageBusPublisher)
                      (implicit db: Database, system: ActorSystem) {
  import Campaign._
  import StatusCodes.{Success => _, _}
  import system.dispatcher

  def cancel(id: Campaign.Id): Route = {
    complete(CampaignLauncher.cancel(id))
  }

  def createCampaign(ns: Namespace, name: CreateCampaign): Route = {
    complete(Created -> db.run(Campaigns.create(ns, name.name)))
  }

  def deleteCampaign(id: Campaign.Id): Route = {
    complete(db.run(Campaigns.delete(id)))
  }

  def fetchCampaign(id: Campaign.Id): Route = {
    complete(db.run(Campaigns.fetch(id)))
  }

  def launch(id: Campaign.Id, lc: LaunchCampaign): Route = {
    lc.isValid match {
      case Xor.Right(()) => complete(CampaignLauncher.launch(deviceRegistry, updateService, id, lc, messageBus))
      case Xor.Left(err) => complete(Conflict -> err)
    }
  }

  def listCampaigns(ns: Namespace): Route = {
    complete(db.run(Campaigns.list(ns)))
  }

  def setAsDraft(id: Campaign.Id): Route = {
    complete(db.run(Campaigns.setAsDraft(id)))
  }

  def setCampaignGroups(id: Campaign.Id, groups: SetCampaignGroups): Route = {
    complete(db.run(Campaigns.setGroups(id, groups.groups)))
  }

  def setCampaignName(id: Campaign.Id, name: CreateCampaign): Route = {
    complete(db.run(Campaigns.setName(id, name.name)))
  }

  def setCampaignPackage(id: Campaign.Id, pkg: PackageId): Route = {
    complete(db.run(Campaigns.setPackage(id, pkg)))
  }

  def campaignAllowed(id: Campaign.Id): Future[Namespace] = {
    db.run(Campaigns.fetchMeta(id).map(_.namespace))
  }

  def getCampaignStats(id: Campaign.Id): Route = {
    complete(CampaignStats.get(id, updateService))
  }

  val extractId: Directive1[Campaign.Id] =
    allowExtractor(namespaceExtractor, extractUuid.map(Campaign.Id(_)), campaignAllowed)

  val route = (ErrorHandler.handleErrors & namespaceExtractor) { ns =>
    val scope = Scopes.campaigns(ns)
    extractExecutionContext { implicit ec =>
      pathPrefix("campaigns") {
        pathEnd {
          scope.get {
            listCampaigns(ns)
          } ~
          (scope.post & entity(as[CreateCampaign])) { name =>
            createCampaign(ns, name)
          }
        } ~
        extractId { id =>
          pathEnd {
            scope.get {
              fetchCampaign(id)
            } ~
            scope.delete {
              deleteCampaign(id)
            }
          } ~
          (path("cancel") & scope.put) {
            cancel(id)
          } ~
          (path("draft") & scope.post) {
            setAsDraft(id)
          } ~
          (path("launch") & scope.post) {
            entity(as[LaunchCampaign]) { launchCampaign =>
              launch(id, launchCampaign)
            } ~ {
              launch(id, LaunchCampaign())
            }
          } ~
          (path("groups") & scope.put & entity(as[SetCampaignGroups])) { groups =>
            setCampaignGroups(id, groups)
          } ~
          (path("name") & scope.put & entity(as[CreateCampaign])) { campName =>
            setCampaignName(id, campName)
          } ~
          (path("package") & scope.put & entity(as[PackageId])) { pkgId =>
            setCampaignPackage(id, pkgId)
          } ~
          (path("statistics") & scope.get) {
            getCampaignStats(id)
          }
        }
      }
    }
  }

}
